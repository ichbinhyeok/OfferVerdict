package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import com.offerverdict.seo.SeoUrlPolicy;
import com.offerverdict.service.ComparisonService;
import com.offerverdict.service.ContentEnrichmentService;
import com.offerverdict.service.RoleGuideService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HubController {
    private static final int MAX_HUB_CITIES = 8;
    private static final int MAX_CITY_ROLE_CARDS = 6;
    private static final double DEFAULT_CITY_MEDIAN_INCOME = 75000.0;
    private static final Map<String, Integer> ROLE_BASELINE_SALARIES = Map.ofEntries(
            Map.entry("software-engineer", 140000),
            Map.entry("data-scientist", 130000),
            Map.entry("product-manager", 150000),
            Map.entry("registered-nurse", 95000),
            Map.entry("accountant", 82000),
            Map.entry("teacher", 72000),
            Map.entry("project-manager", 110000),
            Map.entry("marketing-manager", 85000),
            Map.entry("pharmacist", 138000),
            Map.entry("physical-therapist", 105000),
            Map.entry("electrician", 68000),
            Map.entry("lineman", 110000),
            Map.entry("sales-representative", 78000));

    private final DataRepository repository;
    private final ComparisonService comparisonService;
    private final ContentEnrichmentService contentEnrichmentService;
    private final RoleGuideService roleGuideService;
    private final AppProperties appProperties;

    public HubController(DataRepository repository,
            ComparisonService comparisonService,
            ContentEnrichmentService contentEnrichmentService,
            RoleGuideService roleGuideService,
            AppProperties appProperties) {
        this.repository = repository;
        this.comparisonService = comparisonService;
        this.contentEnrichmentService = contentEnrichmentService;
        this.roleGuideService = roleGuideService;
        this.appProperties = appProperties;
    }

    @GetMapping("/cities")
    public String cities(Model model) {
        List<CityCostEntry> cities = repository.getCities().stream()
                .sorted(Comparator.comparing(CityCostEntry::getPriority) // Priority sort first
                        .thenComparing(CityCostEntry::getCity))
                .toList();

        // Also provide jobs for the directory
        var jobs = repository.getJobs();

        model.addAttribute("cities", cities);
        model.addAttribute("jobs", jobs);
        model.addAttribute("title", "OfferVerdict Directory: Cities & Jobs");
        model.addAttribute("metaDescription", "Browse cost of living analyses by city or job title.");
        model.addAttribute("shouldIndex", false);
        return "cities";
    }

    @GetMapping("/city/{citySlug}")
    public String cityHub(@PathVariable String citySlug, Model model) {
        CityCostEntry city = repository.getCity(citySlug);
        if (city == null) {
            return "redirect:/cities";
        }

        List<Map<String, String>> roleCards = new ArrayList<>();
        for (RoleGuideService.RoleGuide roleGuide : roleGuideService.featuredGuides().stream()
                .limit(MAX_CITY_ROLE_CARDS)
                .toList()) {
            JobInfo job = repository.getJob(roleGuide.slug());
            int benchmarkSalary = benchmarkSalary(job, city);
            Map<String, String> roleCard = new LinkedHashMap<>();
            roleCard.put("title", roleGuide.title());
            roleCard.put("summary", roleGuide.summary());
            roleCard.put("salaryLabel", String.format("$%,d", benchmarkSalary));
            roleCard.put("analysisUrl", "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + benchmarkSalary);
            roleCard.put("comparisonUrl", buildComparisonUrl(job, city));
            roleCard.put("comparisonLabel", buildComparisonLabel(city));
            roleCards.add(roleCard);
        }

        model.addAttribute("city", city);
        model.addAttribute("roleCards", roleCards);
        model.addAttribute("cityContext", contentEnrichmentService.getCityContext(city.getSlug()).orElse(null));
        model.addAttribute("title", city.getCity() + " relocation and salary guide | OfferVerdict");
        model.addAttribute("metaDescription",
                "Check whether a move to " + city.getCity()
                        + " is worth it after tax, rent, and role-specific salary benchmarks.");
        model.addAttribute("canonicalUrl", comparisonService.buildCanonicalUrl("/city/" + city.getSlug()));
        model.addAttribute("shouldIndex", city.getPriority() <= 2);
        model.addAttribute("decisionGuideUrl", "/relocation-salary-calculator");
        model.addAttribute("salaryCheckGuideUrl", "/is-this-salary-enough");

        return "city-directory";
    }

    // New Route: Simple Job Directory (Sitemap style, no complex ranking)
    @GetMapping("/job/{jobSlug}")
    public String jobHub(@PathVariable String jobSlug, Model model) {
        var job = repository.getJob(jobSlug);
        if (job == null)
            return "redirect:/cities";

        List<CityCostEntry> cities = repository.getCities().stream()
                .filter(c -> c.getPriority() <= 2) // Priority cities only for the list
                .sorted(Comparator.comparingInt(CityCostEntry::getPriority)
                        .thenComparing(CityCostEntry::getCity))
                .limit(MAX_HUB_CITIES)
                .toList();

        List<Map<String, String>> cityCards = new ArrayList<>();
        for (CityCostEntry city : cities) {
            int benchmarkSalary = benchmarkSalary(job, city);
            Map<String, String> cityCard = new LinkedHashMap<>();
            cityCard.put("cityName", city.getCity() + ", " + city.getState());
            cityCard.put("state", city.getState());
            cityCard.put("salaryLabel", String.format("$%,d", benchmarkSalary));
            cityCard.put("analysisUrl", "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + benchmarkSalary);
            cityCard.put("comparisonUrl", buildComparisonUrl(job, city));
            cityCard.put("comparisonLabel", buildComparisonLabel(city));
            cityCards.add(cityCard);
        }

        model.addAttribute("job", job);
        model.addAttribute("cities", cities);
        model.addAttribute("cityCards", cityCards);
        model.addAttribute("title", job.getTitle() + " Salary & Cost of Living by City");
        model.addAttribute("metaDescription",
                "Compare " + job.getTitle() + " salary benchmarks, relocation tradeoffs, and cost of living across major US cities.");
        model.addAttribute("canonicalUrl", comparisonService.buildCanonicalUrl("/job/" + job.getSlug()));
        model.addAttribute("shouldIndex", job.isMajor());
        model.addAttribute("jobContext", contentEnrichmentService.getJobContext(job.getSlug()).orElse(null));
        model.addAttribute("roleGuide", roleGuideService.findGuide(job.getSlug()).orElse(null));
        model.addAttribute("decisionGuideUrl", "/job-offer-comparison-calculator");
        model.addAttribute("salaryCheckGuideUrl", "/is-this-salary-enough");

        return "job-directory";
    }

    private int benchmarkSalary(JobInfo job, CityCostEntry city) {
        double suggestedSalary = localizedRoleBenchmark(job, city);
        return SeoUrlPolicy.clampAndAlignSalary((int) Math.round(suggestedSalary),
                appProperties.getSeoSalaryBucketMin(),
                appProperties.getSeoSalaryBucketMax(),
                appProperties.getSeoSalaryBucketInterval());
    }

    private double localizedRoleBenchmark(JobInfo job, CityCostEntry city) {
        double roleBaseline = ROLE_BASELINE_SALARIES.getOrDefault(job.getSlug(), 78000);
        double cityMedianIncome = city.getMedianIncome() > 0 ? city.getMedianIncome() : DEFAULT_CITY_MEDIAN_INCOME;
        double costIndexFactor = clamp(city.getColIndex() / 100.0, 0.88, 1.35);
        double incomeFactor = clamp(cityMedianIncome / DEFAULT_CITY_MEDIAN_INCOME, 0.88, 1.3);

        double cityAdjustedBaseline = roleBaseline * (0.62 * costIndexFactor + 0.38 * incomeFactor);
        double affordabilityGuardrail = cityMedianIncome * roleMedianMultiplier(job.getSlug());

        return Math.max(cityAdjustedBaseline, affordabilityGuardrail);
    }

    private double roleMedianMultiplier(String jobSlug) {
        return switch (jobSlug) {
            case "software-engineer" -> 1.58;
            case "data-scientist" -> 1.48;
            case "product-manager" -> 1.62;
            case "registered-nurse" -> 1.16;
            case "pharmacist" -> 1.6;
            case "project-manager" -> 1.24;
            case "marketing-manager" -> 1.08;
            case "accountant" -> 1.04;
            case "teacher" -> 0.9;
            case "physical-therapist" -> 1.18;
            case "lineman" -> 1.32;
            case "electrician" -> 0.98;
            case "sales-representative" -> 1.02;
            default -> 1.0;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String buildComparisonUrl(JobInfo job, CityCostEntry currentCity) {
        CityCostEntry peerCity = repository.getCities().stream()
                .filter(city -> city.getPriority() <= 2)
                .filter(city -> !city.getSlug().equals(currentCity.getSlug()))
                .sorted(Comparator
                        .<CityCostEntry, Boolean>comparing(city -> !city.getState().equals(currentCity.getState()))
                        .thenComparingInt(CityCostEntry::getPriority)
                        .thenComparing(CityCostEntry::getCity))
                .findFirst()
                .orElse(null);

        if (peerCity == null) {
            return "/job-offer-comparison-calculator";
        }
        return "/" + job.getSlug() + "-salary-" + currentCity.getSlug() + "-vs-" + peerCity.getSlug();
    }

    private String buildComparisonLabel(CityCostEntry currentCity) {
        CityCostEntry peerCity = repository.getCities().stream()
                .filter(city -> city.getPriority() <= 2)
                .filter(city -> !city.getSlug().equals(currentCity.getSlug()))
                .sorted(Comparator
                        .<CityCostEntry, Boolean>comparing(city -> !city.getState().equals(currentCity.getState()))
                        .thenComparingInt(CityCostEntry::getPriority)
                        .thenComparing(CityCostEntry::getCity))
                .findFirst()
                .orElse(null);

        if (peerCity == null) {
            return "Compare offers";
        }
        return "Compare " + currentCity.getCity() + " vs " + peerCity.getCity();
    }
}
