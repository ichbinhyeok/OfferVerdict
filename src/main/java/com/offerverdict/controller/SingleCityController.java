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
import com.offerverdict.service.ComparisonService;
import com.offerverdict.service.DynamicContentService;
import com.offerverdict.service.SingleCityAnalysisService;
import com.offerverdict.service.VerdictAdviser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @GetMapping("/salary-check/{citySlug}/{salaryInt}")
    public Object singleCityAnalysis(@PathVariable("citySlug") String citySlug,
            @PathVariable("salaryInt") int salaryInt,
            Model model) {

        // 1. SEO Rounding Check (301 Redirect)
        int interval = appProperties.getSeoSalaryBucketInterval();
        boolean isAligned = (salaryInt % interval == 0);

        if (!isAligned) {
            int roundedSalary = (int) (Math.round((double) salaryInt / interval) * interval);
            if (roundedSalary == 0)
                roundedSalary = interval; // Avoid 0

            String redirectUrl = "/salary-check/" + citySlug + "/" + roundedSalary;
            RedirectView redirectView = new RedirectView(redirectUrl);
            redirectView.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

        // 2. Load Data
        CityCostEntry city = repository.getCity(citySlug);
        if (city == null) {
            // Spring Boot usually handles 404 if template not found, or define error page
            return "error/404";
        }
        AuthoritativeMetrics metrics = repository.getAuthoritativeMetrics();

        // 3. Analyze (Default Parameters used for landing page)
        ComparisonBreakdown result = analysisService.analyze(
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

        // 4. Determine Verdict (Is this good?)
        // Heuristic: Is Residual > 20% of Net? Or > $1000?
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
        if (salaryInt > interval) {
            prevSalaryUrl = "/salary-check/" + citySlug + "/" + (salaryInt - interval);
        }
        nextSalaryUrl = "/salary-check/" + citySlug + "/" + (salaryInt + interval);

        // 6b. State-based City Links (Internal Linking Grid)
        List<CityCostEntry> relatedCities = repository.getCities().stream()
                .filter(c -> c.getState().equalsIgnoreCase(city.getState()) && !c.getSlug().equals(citySlug))
                .limit(5)
                .toList();

        // 7. Data for Template
        model.addAttribute("city", city);
        model.addAttribute("result", result);
        model.addAttribute("salary", salaryInt);
        model.addAttribute("verdict", verdict);
        model.addAttribute("verdictMsg", verdictMsg);
        model.addAttribute("introText", introText);
        model.addAttribute("housingWarning", housingWarning);
        model.addAttribute("analysisText", analysisText);

        model.addAttribute("prevSalaryUrl", prevSalaryUrl);
        model.addAttribute("nextSalaryUrl", nextSalaryUrl);
        model.addAttribute("relatedCities", relatedCities);
        model.addAttribute("compareUrl", "/software-engineer-salary-" + citySlug + "-vs-austin-tx"); // Default CTA

        // 7b. City Context Enrichment (New enhancement)
        enrichmentService.getCityContext(citySlug).ifPresent(ctx -> model.addAttribute("cityContext", ctx));

        // Legal Shield: Contextual Disclaimer
        model.addAttribute("contextualDisclaimer",
                "*Figures are estimates based on public data. Actual costs vary by neighborhood and lifestyle.");

        // SEO Meta
        model.addAttribute("title", "Is $" + salaryInt + " a good salary in " + city.getCity() + "? - OfferVerdict");
        model.addAttribute("metaDescription", "See the real take-home pay and lifestyle breakdown for a $" + salaryInt
                + " salary in " + city.getCity() + ", " + city.getState() + ".");
        model.addAttribute("canonicalUrl",
                comparisonService.buildCanonicalUrl("/salary-check/" + citySlug + "/" + salaryInt));

        return "single-verdict";
    }
}
