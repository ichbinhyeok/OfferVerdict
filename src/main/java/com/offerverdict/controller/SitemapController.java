package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Comparator;
import java.util.Set;

@Controller
public class SitemapController {
    private static final int MAX_CITY_COUNT = 15;
    private static final int MAX_COMPARISON_SEEDS = 24;
    private static final List<String> CORE_JOB_SLUGS = List.of(
            "software-engineer",
            "product-manager",
            "data-scientist",
            "financial-analyst",
            "registered-nurse",
            "marketing-manager",
            "sales-manager",
            "mechanical-engineer");

    private static final List<String> CORE_COMPARISON_JOBS = List.of(
            "software-engineer",
            "financial-analyst",
            "registered-nurse");


    private final DataRepository repository;
    private final AppProperties appProperties;

    public SitemapController(DataRepository repository, AppProperties appProperties) {
        this.repository = repository;
        this.appProperties = appProperties;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static Pages
        addUrl(xml, "/", "1.0");
        addUrl(xml, "/should-i-take-this-offer", "0.9");
        addUrl(xml, "/job-offer-comparison-calculator", "0.9");
        addUrl(xml, "/relocation-salary-calculator", "0.9");
        addUrl(xml, "/is-this-salary-enough", "0.8");
        addUrl(xml, "/job/software-engineer", "0.8");
        addUrl(xml, "/job/registered-nurse", "0.8");
        addUrl(xml, "/job/product-manager", "0.8");
        addUrl(xml, "/about", "0.8");
        addUrl(xml, "/methodology", "0.8");

        // Focus sitemap on high-intent, high-quality seeds first.
        List<CityCostEntry> seedCities = repository.getCities().stream()
                .sorted(Comparator.comparingInt(CityCostEntry::getTier)
                        .thenComparingInt(CityCostEntry::getPriority)
                        .thenComparing(CityCostEntry::getCity))
                .limit(MAX_CITY_COUNT)
                .toList();
        Set<String> coreJobSet = new LinkedHashSet<>(CORE_JOB_SLUGS);
        List<JobInfo> coreJobs = repository.getJobs().stream()
                .filter(job -> coreJobSet.contains(job.getSlug()))
                .sorted(Comparator.comparingInt(job -> CORE_JOB_SLUGS.indexOf(job.getSlug())))
                .toList();

        int salaryInterval = Math.max(1, appProperties.getSeoSalaryBucketInterval());
        int[] primarySalaryPoints = alignToSeoInterval(new int[] { 60000, 80000, 100000, 120000, 150000 },
                salaryInterval);

        for (CityCostEntry city : seedCities) {
            for (JobInfo job : coreJobs) {
                for (int s : primarySalaryPoints) {
                    addUrl(xml, "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + s, "0.9");
                }
            }
        }

        int comparisonCount = 0;
        List<CityCostEntry> comparisonCities = seedCities.stream().limit(8).toList();
        for (String jobSlug : CORE_COMPARISON_JOBS) {
            for (int i = 0; i < comparisonCities.size(); i++) {
                for (int j = i + 1; j < comparisonCities.size(); j++) {
                    addUrl(xml,
                            "/" + jobSlug + "-salary-" + comparisonCities.get(i).getSlug() + "-vs-"
                                    + comparisonCities.get(j).getSlug(),
                            "0.8");
                    comparisonCount++;
                    if (comparisonCount >= MAX_COMPARISON_SEEDS) {
                        break;
                    }
                }
                if (comparisonCount >= MAX_COMPARISON_SEEDS) {
                    break;
                }
            }
            if (comparisonCount >= MAX_COMPARISON_SEEDS) {
                break;
            }
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String path, String priority) {
        String baseUrl = appProperties.getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append(path).append("</loc>\n");
        xml.append("    <changefreq>weekly</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private int[] alignToSeoInterval(int[] salaries, int interval) {
        Set<Integer> aligned = new LinkedHashSet<>();
        for (int salary : salaries) {
            int rounded = (int) (Math.round((double) salary / interval) * interval);
            if (rounded <= 0) {
                rounded = interval;
            }
            aligned.add(rounded);
        }
        return aligned.stream().mapToInt(Integer::intValue).toArray();
    }
}
