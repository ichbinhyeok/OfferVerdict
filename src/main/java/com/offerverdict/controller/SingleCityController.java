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
import com.offerverdict.seo.SeoUrlPolicy;
import com.offerverdict.service.SingleCityAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class SingleCityController {
    private static final int MAX_INDEXABLE_CITY_PRIORITY = 2;

    private final DataRepository repository;
    private final SingleCityAnalysisService analysisService;
    private final ComparisonService comparisonService; // For canonical helper
    private final DynamicContentService dynamicContentService;
    private final AppProperties appProperties;
    private final com.offerverdict.service.ContentEnrichmentService enrichmentService;
    private final ObjectMapper objectMapper;

    public SingleCityController(DataRepository repository,
            SingleCityAnalysisService analysisService,
            ComparisonService comparisonService,
            DynamicContentService dynamicContentService,
            AppProperties appProperties,
            com.offerverdict.service.ContentEnrichmentService enrichmentService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.analysisService = analysisService;
        this.comparisonService = comparisonService;
        this.dynamicContentService = dynamicContentService;
        this.appProperties = appProperties;
        this.enrichmentService = enrichmentService;
        this.objectMapper = objectMapper;
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
        // Response kept for future cache tuning; avoid synthetic Last-Modified headers.
        String analysisDateUtc = LocalDate.now(ZoneOffset.UTC).toString();

        // 1. SEO salary boundary and rounding checks (301 Redirect)
        int interval = appProperties.getSeoSalaryBucketInterval();
        int minSalary = appProperties.getSeoSalaryBucketMin();
        int maxSalary = appProperties.getSeoSalaryBucketMax();

        if (salaryInt < minSalary || salaryInt > maxSalary) {
            int boundedSalary = SeoUrlPolicy.clampAndAlignSalary(salaryInt, minSalary, maxSalary, interval);
            String redirectUrl = (jobSlug != null)
                    ? "/salary-check/" + jobSlug + "/" + citySlug + "/" + boundedSalary
                    : "/salary-check/" + citySlug + "/" + boundedSalary;

            RedirectView redirectView = new RedirectView(redirectUrl);
            redirectView.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

        boolean isAligned = (salaryInt % interval == 0);

        if (!isAligned) {
            int roundedSalary = SeoUrlPolicy.alignToInterval(salaryInt, interval);

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
            jobInfo = repository.findJobLoosely(jobSlug)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "Job not found"));
            // SEO FIX: If job slug is invalid, we MUST 404 to avoid infinite duplicate
            // content URLs.
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

        String verdictMsg = dynamicContentService.generateSingleCityVerdictMessage(verdict, result);

        // 5. Dynamic Content: deterministic, data-driven copy.
        String introText = dynamicContentService.generateSingleCityIntro(result);
        String housingWarning = dynamicContentService.generateHousingWarning(result);
        String analysisText = dynamicContentService.generateVerdictAnalysis(verdict, result);

        // 5b. FAQ content shared by JSON-LD and visible FAQ section.
        String residualImpact = (result.getResidual() < 0)
                ? String.format("an estimated monthly deficit of $%,.0f", Math.abs(result.getResidual()))
                : String.format("about $%,.0f left each month", result.getResidual());
        String faqQ1 = String.format("Is $%,d a good salary in %s?", salaryInt, city.getCity());
        String faqA1 = String.format("Under current assumptions, this salary results in %s after taxes and core costs in %s.",
                residualImpact, city.getCity());

        String faqQ2 = String.format("What is the estimated take-home pay for $%,d in %s?", salaryInt, city.getCity());
        String faqA2 = String.format("Estimated annual take-home pay is about $%,.0f before lifestyle-specific adjustments.",
                result.getNetMonthly() * 12.0);

        double housingRatio = result.getNetMonthly() > 0 ? result.getRent() / result.getNetMonthly() : 0.0;
        String housingLoad;
        if (housingRatio >= 0.45) {
            housingLoad = "high";
        } else if (housingRatio >= 0.35) {
            housingLoad = "elevated";
        } else {
            housingLoad = "moderate";
        }
        String faqQ3 = String.format("Is %s expensive for housing?", city.getCity());
        String faqA3 = String.format(Locale.US,
                "Average rent used in this model is about $%,.0f per month in %s (roughly %.0f%% of estimated take-home pay), indicating a %s housing burden for many single-income households.",
                result.getRent(), city.getCity(), housingRatio * 100.0, housingLoad);

        // 6. Navigation Neighbors (Previous/Next Salary)
        String prevSalaryUrl = null;
        String nextSalaryUrl = null;

        String urlPrefix = "/salary-check/";
        if (jobSlug != null) {
            urlPrefix += jobSlug + "/";
        }

        if (salaryInt - interval >= minSalary) {
            prevSalaryUrl = urlPrefix + citySlug + "/" + (salaryInt - interval);
        }
        if (salaryInt + interval <= maxSalary) {
            nextSalaryUrl = urlPrefix + citySlug + "/" + (salaryInt + interval);
        }

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
        model.addAttribute("faqQ1", faqQ1);
        model.addAttribute("faqA1", faqA1);
        model.addAttribute("faqQ2", faqQ2);
        model.addAttribute("faqA2", faqA2);
        model.addAttribute("faqQ3", faqQ3);
        model.addAttribute("faqA3", faqA3);

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

        // SEO FIX: Add index gating to avoid indexing outlier salaries
        boolean shouldIndex = (salaryInt >= minSalary && salaryInt <= maxSalary);
        if (jobInfo == null) {
            shouldIndex = false;
        }
        if (jobInfo != null && "Custom".equalsIgnoreCase(jobInfo.getCategory())) {
            shouldIndex = false;
        }
        if (jobInfo != null && !jobInfo.isMajor()) {
            shouldIndex = false;
        }
        if (city.getPriority() > MAX_INDEXABLE_CITY_PRIORITY) {
            shouldIndex = false;
        }
        model.addAttribute("shouldIndex", shouldIndex);

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
            if (Math.abs(position) < 0.5) {
                model.addAttribute("marketPositionText", "in line with the local median");
            } else if (position > 0) {
                model.addAttribute("marketPositionText",
                        String.format("about %.1f%% above the local median", Math.abs(position)));
            } else {
                model.addAttribute("marketPositionText",
                        String.format("about %.1f%% below the local median", Math.abs(position)));
            }

            // Calculate progress bar left position (0-100 range)
            double barLeft = Math.min(100.0, Math.max(0.0, 50.0 + (position / 2.0)));
            model.addAttribute("marketBarLeft", barLeft);
            model.addAttribute("jobTitle", (jobInfo != null) ? jobInfo.getTitle() : "all occupations");
        } else {
            model.addAttribute("jobTitle", "all occupations");
        }

        // 7f. Relocation ROI (Relational Baseline)
        try {
            double sfBaselineSalary = appProperties.getRelocationBaselineSalarySf();
            double nycBaselineSalary = appProperties.getRelocationBaselineSalaryNyc();
            model.addAttribute("relocationBaselineSalarySf", sfBaselineSalary);
            model.addAttribute("relocationBaselineSalaryNyc", nycBaselineSalary);

            // SF Baseline
            ComparisonResult sfBaseline = comparisonService.compare("san-francisco-ca", citySlug, sfBaselineSalary,
                    (double) salaryInt,
                    HouseholdType.SINGLE, HousingType.RENT, false, 0.0, 0.0, 0.0, 0.0, false, true);
            double sfResidual = sfBaseline.getCurrent().getResidual();
            double targetResidual = sfBaseline.getOffer().getResidual();
            double monthlyGainVsSF = targetResidual - sfResidual;

            // NYC Baseline
            ComparisonResult nycBaseline = comparisonService.compare("new-york-ny", citySlug, nycBaselineSalary,
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
            model.addAttribute("compareUrl", buildComparisonDestination(jobInfo, city));
            model.addAttribute("compareLabel", buildComparisonLabel(jobInfo, city));
            model.addAttribute("breadcrumbUrl", "/job/" + jobInfo.getSlug());
            model.addAttribute("breadcrumbLabel", jobInfo.getTitle() + " guide");
        } else {
            model.addAttribute("compareUrl", "/relocation-salary-calculator");
            model.addAttribute("compareLabel", "Compare relocation scenarios");
            model.addAttribute("breadcrumbUrl", "/relocation-salary-calculator");
            model.addAttribute("breadcrumbLabel", "Relocation guide");
        }

        // Legal Shield
        model.addAttribute("contextualDisclaimer",
                "*Figures are estimates based on public data. Actual costs vary by neighborhood and lifestyle.");
        String dataSourceSummary = "IRS tax tables, BLS wage benchmarks, and Numbeo cost-of-living inputs";
        String dataLastUpdated = "See /methodology for source update dates";
        if (metrics != null && metrics.getMetadata() != null) {
            if (metrics.getMetadata().source != null && !metrics.getMetadata().source.isBlank()) {
                dataSourceSummary = metrics.getMetadata().source;
            }
            if (metrics.getMetadata().lastUpdated != null && !metrics.getMetadata().lastUpdated.isBlank()) {
                dataLastUpdated = metrics.getMetadata().lastUpdated;
            }
        }
        model.addAttribute("dataSourceSummary", dataSourceSummary);
        model.addAttribute("dataLastUpdated", dataLastUpdated);
        model.addAttribute("analysisDateUtc", analysisDateUtc);

        // --- SMART CROSS-LINKING (SEO Siloing) ---
        // 1. Salary Neighbors (+/- 10k, 20k)
        Map<String, String> salaryLinks = new java.util.LinkedHashMap<>();
        int[] steps = { -20000, -10000, 10000, 20000 };
        String jobSegment = (jobInfo != null) ? jobInfo.getSlug() + "/" : "";

        for (int step : steps) {
            int newSalary = salaryInt + step;
            if (newSalary > 30000) { // Min salary check
                salaryLinks.put(String.format("$%,d", newSalary),
                        "/salary-check/" + jobSegment + citySlug + "/" + newSalary);
            }
        }
        model.addAttribute("salaryLinks", salaryLinks);

        // 2. City Neighbors (Same State or Popular)
        Map<String, String> cityLinks = new java.util.LinkedHashMap<>();
        for (CityCostEntry c : relatedCities) {
            if (c.getPriority() > MAX_INDEXABLE_CITY_PRIORITY) {
                continue;
            }
            cityLinks.put(c.getCity() + ", " + c.getState(),
                    "/salary-check/" + jobSegment + c.getSlug() + "/" + salaryInt);
            if (cityLinks.size() >= 4)
                break;
        }
        // Fallback to top cities if not enough related cities
        if (cityLinks.size() < 4) {
            List<String> fallbacks = List.of("austin-tx", "dallas-tx", "new-york-ny", "seattle-wa");
            for (String fSlug : fallbacks) {
                if (!fSlug.equals(citySlug) && !cityLinks.values().stream().anyMatch(v -> v.contains(fSlug))) {
                    cityLinks.put(fSlug.replace("-", " ").toUpperCase(),
                            "/salary-check/" + jobSegment + fSlug + "/" + salaryInt);
                }
                if (cityLinks.size() >= 4)
                    break;
            }
        }
        model.addAttribute("cityLinks", cityLinks);

        // 3. Job Neighbors (Link to other roles in same city)
        Map<String, String> jobLinks = new java.util.LinkedHashMap<>();
        if (jobInfo != null) {
            List<JobInfo> relatedJobs = repository.getRelatedJobs(jobInfo.getCategory(), jobInfo.getSlug(), 4);
            for (JobInfo j : relatedJobs) {
                if (!j.isMajor()) {
                    continue;
                }
                jobLinks.put(j.getTitle(), "/salary-check/" + j.getSlug() + "/" + citySlug + "/" + salaryInt);
            }
            // Fallback if category results are empty
            if (jobLinks.isEmpty()) {
                List<String> otherJobs = List.of("product-manager", "data-scientist", "software-engineer",
                        "marketing-manager");
                for (String jSlug : otherJobs) {
                    if (!jSlug.equals(jobInfo.getSlug())) {
                        jobLinks.put(jSlug.replace("-", " ").toUpperCase(),
                                "/salary-check/" + jSlug + "/" + citySlug + "/" + salaryInt);
                    }
                    if (jobLinks.size() >= 4)
                        break;
                }
            }
            model.addAttribute("jobLinks", jobLinks);
        }

        // SEO Meta
        model.addAttribute("title",
                generateRiskBasedTitle(city, salaryInt, jobInfo));
        model.addAttribute("metaDescription",
                generateRiskBasedDescription(city, result, salaryInt, jobInfo, analysisDateUtc));

        // SEO Structured Data (Breadcrumb)
        String canonicalPath = (jobSlug != null)
                ? "/salary-check/" + jobSlug + "/" + citySlug + "/" + salaryInt
                : "/salary-check/" + citySlug + "/" + salaryInt;

        String canonicalUrl = comparisonService.buildCanonicalUrl(canonicalPath);

        // Build Breadcrumb JSON-LD
        String jobName = (jobInfo != null) ? jobInfo.getTitle() : "Salary";
        String breadcrumbName = String.format("$%d %s in %s", salaryInt, jobName, city.getCity());
        List<Map<String, String>> faqItems = List.of(
                Map.of("question", faqQ1, "answer", faqA1),
                Map.of("question", faqQ2, "answer", faqA2),
                Map.of("question", faqQ3, "answer", faqA3));
        model.addAttribute("structuredDataJson", toJson(buildStructuredData(canonicalUrl, breadcrumbName, faqItems)));
        model.addAttribute("canonicalUrl", canonicalUrl);

        return "single-verdict";
    }

    private String buildComparisonDestination(JobInfo jobInfo, CityCostEntry city) {
        if (jobInfo == null || !jobInfo.isMajor()) {
            return "/job-offer-comparison-calculator";
        }

        CityCostEntry peerCity = findPeerCity(city);
        if (peerCity == null) {
            return "/job/" + jobInfo.getSlug();
        }
        return "/" + jobInfo.getSlug() + "-salary-" + city.getSlug() + "-vs-" + peerCity.getSlug();
    }

    private String buildComparisonLabel(JobInfo jobInfo, CityCostEntry city) {
        if (jobInfo == null || !jobInfo.isMajor()) {
            return "Compare job offers";
        }

        CityCostEntry peerCity = findPeerCity(city);
        if (peerCity == null) {
            return "See more " + jobInfo.getTitle() + " scenarios";
        }
        return "Compare " + city.getCity() + " vs " + peerCity.getCity();
    }

    private CityCostEntry findPeerCity(CityCostEntry city) {
        return repository.getCities().stream()
                .filter(candidate -> candidate.getPriority() <= MAX_INDEXABLE_CITY_PRIORITY)
                .filter(candidate -> !candidate.getSlug().equals(city.getSlug()))
                .sorted(java.util.Comparator
                        .<CityCostEntry, Boolean>comparing(candidate -> !candidate.getState().equals(city.getState()))
                        .thenComparingInt(CityCostEntry::getPriority)
                        .thenComparing(CityCostEntry::getCity))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> buildStructuredData(String canonicalUrl, String breadcrumbName,
            List<Map<String, String>> faqItems) {
        List<Map<String, Object>> graph = new ArrayList<>();

        Map<String, Object> breadcrumb = new LinkedHashMap<>();
        breadcrumb.put("@context", "https://schema.org");
        breadcrumb.put("@type", "BreadcrumbList");
        breadcrumb.put("itemListElement", List.of(
                Map.of(
                        "@type", "ListItem",
                        "position", 1,
                        "name", "Home",
                        "item", comparisonService.buildCanonicalUrl("/")),
                Map.of(
                        "@type", "ListItem",
                        "position", 2,
                        "name", "Salary Check",
                        "item", comparisonService.buildCanonicalUrl("/")),
                Map.of(
                        "@type", "ListItem",
                        "position", 3,
                        "name", breadcrumbName,
                        "item", canonicalUrl)));
        graph.add(breadcrumb);

        List<Map<String, Object>> questions = new ArrayList<>();
        for (Map<String, String> faqItem : faqItems) {
            questions.add(Map.of(
                    "@type", "Question",
                    "name", faqItem.getOrDefault("question", ""),
                    "acceptedAnswer", Map.of(
                            "@type", "Answer",
                            "text", faqItem.getOrDefault("answer", ""))));
        }

        Map<String, Object> faq = new LinkedHashMap<>();
        faq.put("@context", "https://schema.org");
        faq.put("@type", "FAQPage");
        faq.put("mainEntity", questions);
        graph.add(faq);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("@graph", graph);
        return data;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String generateRiskBasedTitle(CityCostEntry city, int salaryInt, JobInfo jobInfo) {
        String currentYear = java.time.Year.now().toString();
        String formattedSalary = String.format("%,d", salaryInt);
        if (jobInfo != null) {
            return String.format(
                    "$%s %s Salary in %s: Take-Home Pay & Cost of Living (%s)",
                    formattedSalary, jobInfo.getTitle(), city.getCity(), currentYear);
        }
        return String.format(
                "$%s Salary in %s: Take-Home Pay & Cost of Living (%s)",
                formattedSalary, city.getCity(), currentYear);
    }

    private String generateRiskBasedDescription(CityCostEntry city, ComparisonBreakdown result, int salaryInt,
            JobInfo jobInfo, String analysisDateUtc) {
        String formattedSalary = String.format("%,d", salaryInt);
        String formattedResidual = String.format("%,d", Math.round(result.getResidual()));
        String formattedResidualAbs = String.format("%,d", Math.round(Math.abs(result.getResidual())));
        String rolePrefix = (jobInfo != null) ? jobInfo.getTitle() + " " : "";
        String description;
        if (result.getResidual() >= 0) {
            description = String.format(
                    "%s$%s salary in %s leaves about $%s/mo after tax, rent, and living costs. Analysis date: %s UTC.",
                    rolePrefix, formattedSalary, city.getCity(), formattedResidual, analysisDateUtc);
        } else {
            description = String.format(
                    "%s$%s salary in %s shows about a $%s/mo gap after tax, rent, and living costs. Analysis date: %s UTC.",
                    rolePrefix, formattedSalary, city.getCity(), formattedResidualAbs, analysisDateUtc);
        }
        return clampMetaDescription(description, 155);
    }

    private String clampMetaDescription(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3).trim() + "...";
    }
}

