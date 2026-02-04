package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.AuthoritativeMetrics;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import com.offerverdict.model.Verdict;
import com.offerverdict.model.JobInfo;
import com.offerverdict.service.ComparisonService;
import com.offerverdict.service.DynamicContentService;
import com.offerverdict.service.SingleCityAnalysisService;
import com.offerverdict.service.VerdictAdviser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

@Controller
public class SingleCityController {

    private final DataRepository repository;
    private final SingleCityAnalysisService analysisService;
    private final ComparisonService comparisonService; // For canonical helper
    private final DynamicContentService dynamicContentService;
    private final AppProperties appProperties;
    private final VerdictAdviser verdictAdviser;
    private final com.offerverdict.service.ContentEnrichmentService enrichmentService;

    public SingleCityController(DataRepository repository,
            SingleCityAnalysisService analysisService,
            ComparisonService comparisonService,
            DynamicContentService dynamicContentService,
            AppProperties appProperties,
            VerdictAdviser verdictAdviser,
            com.offerverdict.service.ContentEnrichmentService enrichmentService) {
        this.repository = repository;
        this.analysisService = analysisService;
        this.comparisonService = comparisonService;
        this.dynamicContentService = dynamicContentService;
        this.appProperties = appProperties;
        this.verdictAdviser = verdictAdviser;
        this.enrichmentService = enrichmentService;
    }

    // --- ENDPOINTS ---

    @GetMapping("/salary-check/{citySlug}/{salaryInt}")
    public Object singleCityAnalysis(@PathVariable("citySlug") String citySlug,
            @PathVariable("salaryInt") int salaryInt,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {
        return processAnalysis(null, citySlug, salaryInt, response, model);
    }

    @GetMapping("/salary-check/{jobSlug}/{citySlug}/{salaryInt}")
    public Object singleCityJobAnalysis(@PathVariable("jobSlug") String jobSlug,
            @PathVariable("citySlug") String citySlug,
            @PathVariable("salaryInt") int salaryInt,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {
        return processAnalysis(jobSlug, citySlug, salaryInt, response, model);
    }

    /**
     * Shared processing logic for both generic and job-specific analysis.
     */
    private Object processAnalysis(String jobSlug, String citySlug,
            int salaryInt,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {

        // SEO SIGNAL: Last-Modified Header
        response.setHeader(org.springframework.http.HttpHeaders.LAST_MODIFIED,
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now()));

        // 1. SEO Rounding Check (301 Redirect)
        int interval = appProperties.getSeoSalaryBucketInterval();
        boolean isAligned = (salaryInt % interval == 0);

        if (!isAligned) {
            int roundedSalary = (int) (Math.round((double) salaryInt / interval) * interval);
            if (roundedSalary == 0)
                roundedSalary = interval; // Avoid 0

            // Preserve jobSlug in redirect if present
            String redirectUrl;
            if (jobSlug != null) {
                redirectUrl = "/salary-check/" + jobSlug + "/" + citySlug + "/" + roundedSalary;
            } else {
                redirectUrl = "/salary-check/" + citySlug + "/" + roundedSalary;
            }

            RedirectView redirectView = new RedirectView(redirectUrl);
            redirectView.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

        // 2. Load Data
        CityCostEntry city = repository.getCity(citySlug);
        if (city == null) {
            return "error/404";
        }
        AuthoritativeMetrics metrics = repository.getAuthoritativeMetrics();

        // 2b. Load Job Data (Optional)
        JobInfo jobInfo = null;
        if (jobSlug != null) {
            jobInfo = repository.findJobLoosely(jobSlug).orElse(null);
            // If job slug is invalid, should we 404 or just ignore?
            // Ideally 404 to avoid duplicate content, but ignoring is safer for now.
        }

        // 3. Analyze (Default Parameters used for landing page)
        ComparisonBreakdown result;
        try {
            result = analysisService.analyze(
                    (double) salaryInt,
                    city,
                    metrics,
                    HouseholdType.SINGLE,
                    HousingType.RENT,
                    false, // not married
                    0.05, // 5% 401k default
                    150.0, // insurance
                    0.0, // no debt default
                    0.0, // extra leaks
                    0.0, // side hustle
                    false, // remote
                    true, // car owner
                    0.0, 0.0, 1.0, 0.0 // no equity/bonus
            );
        } catch (Exception e) {
            // FALLBACK MECHANISM to prevent 500 Error
            result = new ComparisonBreakdown();
            result.setCityName(city.getCity());
            result.setGrossSalary((double) salaryInt);
            result.setNetMonthly(salaryInt * 0.75 / 12.0); // Rough estimate
            result.setResidual(salaryInt * 0.2 / 12.0); // Rough estimate

            // Populate all fields to prevent template format errors
            double safeRent = 2000.0;
            result.setRent(safeRent);
            result.setLivingCost(1500.0);
            result.setGroceries(600.0);
            result.setTransport(500.0);
            result.setUtilities(300.0);
            result.setMisc(100.0);
            result.setInsurance(150.0);
            result.setLocalTax(0.0);

            // Fix TaxResult
            com.offerverdict.service.TaxCalculatorService.TaxResult safeTax = new com.offerverdict.service.TaxCalculatorService.TaxResult();
            safeTax.setFederalTax((salaryInt * 0.20) / 12);
            safeTax.setStateTax((salaryInt * 0.05) / 12);
            safeTax.setFicaTax((salaryInt * 0.0765) / 12);
            result.setTaxResult(safeTax);
        }

        // 4. Determine Verdict (Is this good?)
        Verdict verdict;
        double residual = result.getResidual();
        double net = result.getNetMonthly();

        if (residual < 0)
            verdict = Verdict.NO_GO;
        else if (residual / net < 0.15)
            verdict = Verdict.WARNING;
        else if (residual / net < 0.30)
            verdict = Verdict.CONDITIONAL;
        else
            verdict = Verdict.GO;

        String verdictMsg = verdictAdviser.generateVerdictMessage(verdict, 0, city.getCity());

        // 5. Dynamic Content
        String introText = dynamicContentService.generateSingleCityIntro(result);
        String housingWarning = dynamicContentService.generateHousingWarning(result);
        String analysisText = dynamicContentService.generateVerdictAnalysis(verdict, result);

        // 6. Navigation Neighbors (Previous/Next Salary)
        String prevSalaryUrl = null;
        String nextSalaryUrl = null;

        String urlPrefix = "/salary-check/";
        if (jobSlug != null) {
            urlPrefix += jobSlug + "/";
        }

        if (salaryInt > interval) {
            prevSalaryUrl = urlPrefix + citySlug + "/" + (salaryInt - interval);
        }
        nextSalaryUrl = urlPrefix + citySlug + "/" + (salaryInt + interval);

        // 6b. State-based City Links (Internal Linking Grid)
        List<CityCostEntry> relatedCities = repository.getRelatedCities(city.getState(), citySlug, 5);

        // 7. PRE-CALCULATE FOR ROBUST TEMPLATE (No logic in HTML)
        double grossMonthly = salaryInt / 12.0;

        double safeTotalTax = 0.0;
        if (result.getTaxResult() != null) {
            safeTotalTax = result.getTaxResult().getFederalTax()
                    + result.getTaxResult().getStateTax()
                    + result.getTaxResult().getFicaTax();
        }
        double safeLocalTax = result.getLocalTax(); // primitive, safe
        double taxMonthly = (safeTotalTax + safeLocalTax) / 12.0;

        double rentMonthly = result.getRent();
        double livingCostMonthly = result.getLivingCost();

        // Gap = Gross - Tax - Rent - Living - Residual
        double gapMonthly = grossMonthly - taxMonthly - rentMonthly - livingCostMonthly - result.getResidual();

        // 7. Data for Template
        model.addAttribute("city", city);
        model.addAttribute("result", result);
        model.addAttribute("salary", salaryInt);
        model.addAttribute("verdict", verdict);
        model.addAttribute("verdictMsg", verdictMsg);
        model.addAttribute("introText", introText);
        model.addAttribute("housingWarning", housingWarning);
        model.addAttribute("analysisText", analysisText);

        // SAFE PRE-CALCULATED VALUES
        model.addAttribute("grossMonthly", grossMonthly);
        model.addAttribute("taxMonthly", taxMonthly);
        model.addAttribute("rentMonthly", rentMonthly);
        model.addAttribute("livingCostMonthly", livingCostMonthly);
        model.addAttribute("gapMonthly", gapMonthly);

        model.addAttribute("prevSalaryUrl", prevSalaryUrl);
        model.addAttribute("nextSalaryUrl", nextSalaryUrl);
        model.addAttribute("relatedCities", relatedCities);
        model.addAttribute("salaryInterval", interval);

        // 7c. Verdict CSS Class
        String verdictCssClass = "neutral-blue";
        if (verdict == Verdict.GO) {
            verdictCssClass = "premium-gold";
        } else if (verdict == Verdict.NO_GO) {
            verdictCssClass = "harsh-red";
        }
        model.addAttribute("verdictCssClass", verdictCssClass);

        // 7e. Market Benchmarking Analysis
        String jSlugForMarket = (jobInfo != null) ? jobInfo.getSlug() : "default";
        Map<String, Double> benchmark = repository.getMarketBenchmark(jSlugForMarket, citySlug);

        if (!benchmark.isEmpty()) {
            double p10 = benchmark.getOrDefault("p10", 0.0);
            double p50 = benchmark.getOrDefault("p50", 0.0);
            double p90 = benchmark.getOrDefault("p90", 0.0);

            String marketRating = "Fair";
            double salary = (double) salaryInt;

            if (salary < p10)
                marketRating = "Low-ball";
            else if (salary >= p90)
                marketRating = "Elite";
            else if (salary >= p50)
                marketRating = "Competitive";

            model.addAttribute("marketMedian", p50);
            model.addAttribute("marketRating", marketRating);

            double position = (salary / p50) * 100 - 100;
            model.addAttribute("marketPosition", position);
            model.addAttribute("marketPositionAbs", Math.abs(position));

            // Calculate progress bar left position (0-100 range)
            double barLeft = Math.min(100.0, Math.max(0.0, 50.0 + (position / 2.0)));
            model.addAttribute("marketBarLeft", barLeft);
            model.addAttribute("jobTitle", (jobInfo != null) ? jobInfo.getTitle() : "Professional role");
        } else {
            model.addAttribute("jobTitle", "Professional role");
        }

        // 7f. Relocation ROI (Relational Baseline)
        try {
            // SF Baseline: $180k
            ComparisonResult sfBaseline = comparisonService.compare("san-francisco-ca", citySlug, 180000.0,
                    (double) salaryInt,
                    HouseholdType.SINGLE, HousingType.RENT, false, 0.0, 0.0, 0.0, 0.0, false, true);
            double sfResidual = sfBaseline.getCurrent().getResidual();
            double targetResidual = sfBaseline.getOffer().getResidual();
            double monthlyGainVsSF = targetResidual - sfResidual;

            // NYC Baseline: $170k
            ComparisonResult nycBaseline = comparisonService.compare("new-york-ny", citySlug, 170000.0,
                    (double) salaryInt,
                    HouseholdType.SINGLE, HousingType.RENT, false, 0.0, 0.0, 0.0, 0.0, false, true);
            double nycResidual = nycBaseline.getCurrent().getResidual();
            double monthlyGainVsNYC = targetResidual - nycResidual;

            model.addAttribute("relocationGainSF", monthlyGainVsSF);
            model.addAttribute("relocationGainNYC", monthlyGainVsNYC);
            model.addAttribute("relocationGainSFAbs", Math.abs(monthlyGainVsSF));
            model.addAttribute("relocationGainNYCAbs", Math.abs(monthlyGainVsNYC));
            model.addAttribute("isGainSF", monthlyGainVsSF > 0);
            model.addAttribute("isGainNYC", monthlyGainVsNYC > 0);

            // Added: Big Mac Index Comparison
            if (metrics != null && metrics.getBigMacIndex() != null) {
                double sfMac = metrics.getBigMacIndex().getOrDefault("San Francisco", 6.50);
                double targetMac = metrics.getBigMacIndex().getOrDefault(city.getCity(),
                        metrics.getBigMacIndex().getOrDefault("nationalAverage", 5.69));
                model.addAttribute("bigMacSF", sfMac);
                model.addAttribute("bigMacTarget", targetMac);
                model.addAttribute("bigMacIsCheaper", targetMac < sfMac);
            }
        } catch (Exception e) {
            // Fallback silently if baseline hubs are missing in data
        }

        // 7b. City Context Enrichment
        enrichmentService.getCityContext(citySlug).ifPresent(ctx -> model.addAttribute("cityContext", ctx));

        // 7d. Job Context Enrichment & CTA Update
        if (jobInfo != null) {
            model.addAttribute("jobInfo", jobInfo);
            enrichmentService.getJobContext(jobInfo.getSlug()).ifPresent(ctx -> model.addAttribute("jobContext", ctx));
            // Update CTA to use actual job
            model.addAttribute("compareUrl", "/" + jobInfo.getSlug() + "-salary-" + citySlug + "-vs-austin-tx");
        } else {
            // Default CTA fallback
            model.addAttribute("compareUrl", "/software-engineer-salary-" + citySlug + "-vs-austin-tx");
        }

        // Legal Shield
        model.addAttribute("contextualDisclaimer",
                "*Figures are estimates based on public data. Actual costs vary by neighborhood and lifestyle.");

        // SEO Meta
        model.addAttribute("title", generateRiskBasedTitle(city, salaryInt, jobInfo));
        model.addAttribute("metaDescription", generateRiskBasedDescription(city, result, salaryInt, jobInfo));

        // SEO Structured Data (Breadcrumb)
        String canonicalPath = (jobSlug != null)
                ? "/salary-check/" + jobSlug + "/" + citySlug + "/" + salaryInt
                : "/salary-check/" + citySlug + "/" + salaryInt;

        String canonicalUrl = comparisonService.buildCanonicalUrl(canonicalPath);

        // Build Breadcrumb JSON-LD
        String jobName = (jobInfo != null) ? jobInfo.getTitle() : "Salary";
        String breadcrumbName = String.format("$%d %s in %s", salaryInt, jobName, city.getCity());

        String structuredData = String.format("""
                {
                  "@context": "https://schema.org",
                  "@type": "BreadcrumbList",
                  "itemListElement": [{
                    "@type": "ListItem",
                    "position": 1,
                    "name": "Home",
                    "item": "%s"
                  },{
                    "@type": "ListItem",
                    "position": 2,
                    "name": "Salary Check",
                    "item": "%s/cities"
                  },{
                    "@type": "ListItem",
                    "position": 3,
                    "name": "%s",
                    "item": "%s"
                  }]
                }
                """, appProperties.getPublicBaseUrl(), appProperties.getPublicBaseUrl(), breadcrumbName, canonicalUrl);

        model.addAttribute("structuredDataJson", structuredData);
        model.addAttribute("canonicalUrl", canonicalUrl);

        return "single-verdict";
    }

    private String generateRiskBasedTitle(CityCostEntry city, int salaryInt, JobInfo jobInfo) {
        String currentYear = java.time.Year.now().toString();
        String jobPrefix = (jobInfo != null) ? jobInfo.getTitle() + " " : "";

        return String.format(
                "Before You Accept That %s %s Offer: Can You Actually Afford Life Here? (%s)",
                city.getCity(), jobPrefix, currentYear);
    }

    private String generateRiskBasedDescription(CityCostEntry city, ComparisonBreakdown result, int salaryInt,
            JobInfo jobInfo) {
        double rentBurden = result.getRent() / (salaryInt / 12.0);
        String jobPrefix = (jobInfo != null) ? " as a " + jobInfo.getTitle() : "";

        // State tax rate analysis
        double stateTaxRate = 0.0;
        if (result.getTaxResult() != null && result.getTaxResult().getStateTax() > 0) {
            stateTaxRate = result.getTaxResult().getStateTax() / salaryInt;
        }

        if (stateTaxRate > 0.05) {
            return String.format(
                    "Don't accept that %s offer%s without seeing this. We calculated the REAL residual after %s's state tax bite. Is there anything left?",
                    city.getCity(), jobPrefix, city.getState());
        } else if (rentBurden > 0.30) {
            return String.format(
                    "Can you actually afford life in %s%s on this salary? We break down the housing burden and savings. The numbers might shock you.",
                    city.getCity(), jobPrefix);
        } else if (rentBurden < 0.20) {
            return String.format(
                    "Is %s's low cost worth the tradeoff%s? Calculate your true purchasing power—taxes, lifestyle, career. Don't just chase cheap rent.",
                    city.getCity(), jobPrefix);
        } else {
            return String.format(
                    "Should you move to %s%s for this salary? We expose the real trade-offs: tax burden vs. lifestyle. Make the call with data.",
                    city.getCity(), jobPrefix);
        }
    }
}
