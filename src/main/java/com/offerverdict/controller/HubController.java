package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import com.offerverdict.seo.SeoUrlPolicy;
import com.offerverdict.service.ComparisonService;
import com.offerverdict.service.ContentEnrichmentService;
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

    private final DataRepository repository;
    private final ComparisonService comparisonService;
    private final ContentEnrichmentService contentEnrichmentService;
    private final AppProperties appProperties;

    public HubController(DataRepository repository,
            ComparisonService comparisonService,
            ContentEnrichmentService contentEnrichmentService,
            AppProperties appProperties) {
        this.repository = repository;
        this.comparisonService = comparisonService;
        this.contentEnrichmentService = contentEnrichmentService;
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
        model.addAttribute("decisionGuideUrl", "/job-offer-comparison-calculator");
        model.addAttribute("salaryCheckGuideUrl", "/is-this-salary-enough");

        return "job-directory";
    }

    private int benchmarkSalary(JobInfo job, CityCostEntry city) {
        Map<String, Double> benchmark = repository.getMarketBenchmark(job.getSlug(), city.getSlug());
        double p50 = benchmark.getOrDefault("p50", 100000.0);
        return SeoUrlPolicy.clampAndAlignSalary((int) Math.round(p50),
                appProperties.getSeoSalaryBucketMin(),
                appProperties.getSeoSalaryBucketMax(),
                appProperties.getSeoSalaryBucketInterval());
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
