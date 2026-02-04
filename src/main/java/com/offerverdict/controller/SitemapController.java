package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import com.offerverdict.service.ComparisonService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class SitemapController {
    private final DataRepository repository;
    private final ComparisonService comparisonService;
    private final AppProperties appProperties;

    public SitemapController(DataRepository repository, ComparisonService comparisonService,
            AppProperties appProperties) {
        this.repository = repository;
        this.comparisonService = comparisonService;
        this.appProperties = appProperties;
    }

    @GetMapping(value = "/sitemap-index.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemapIndex() {
        List<SitemapUrl> urls = buildComparisonUrls();
        int chunkSize = appProperties.getSitemapChunkSize();
        // default chunk size 5000 if not set
        if (chunkSize <= 0)
            chunkSize = 5000;

        int totalChunks = (int) Math.ceil(urls.size() / (double) chunkSize);
        List<String> locs = new ArrayList<>();
        for (int i = 1; i <= totalChunks; i++) {
            String loc = comparisonService.buildCanonicalUrl("/sitemap-" + i + ".xml");
            locs.add(loc);
        }
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        for (String loc : locs) {
            xml.append("<sitemap><loc>").append(loc).append("</loc></sitemap>");
        }
        xml.append("</sitemapindex>");
        return respondWithCache(xml.toString());
    }

    @GetMapping(value = "/sitemap-{page}.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemapPage(@PathVariable int page) {
        List<SitemapUrl> urls = buildComparisonUrls();
        int chunkSize = appProperties.getSitemapChunkSize();
        if (chunkSize <= 0)
            chunkSize = 5000;

        int from = Math.max(0, (page - 1) * chunkSize);
        if (from >= urls.size()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
        int to = Math.min(urls.size(), from + chunkSize);
        List<SitemapUrl> slice = urls.subList(from, to);

        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        for (SitemapUrl url : slice) {
            xml.append("<url>")
                    .append("<loc>").append(url.loc).append("</loc>")
                    .append("<lastmod>").append(url.lastmod).append("</lastmod>")
                    .append("<priority>").append(url.priority).append("</priority>")
                    .append("<changefreq>").append(url.changefreq).append("</changefreq>")
                    .append("</url>");
        }
        xml.append("</urlset>");
        return respondWithCache(xml.toString());
    }

    /**
     * Inner class to hold URL metadata for SEO-optimized sitemaps
     */
    private static class SitemapUrl {
        String loc;
        String lastmod;
        String priority;
        String changefreq;

        SitemapUrl(String loc, String lastmod, String priority, String changefreq) {
            this.loc = loc;
            this.lastmod = lastmod;
            this.priority = priority;
            this.changefreq = changefreq;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SitemapUrl that = (SitemapUrl) o;
            return loc.equals(that.loc);
        }

        @Override
        public int hashCode() {
            return loc.hashCode();
        }
    }

    private List<SitemapUrl> buildComparisonUrls() {
        List<JobInfo> jobs = repository.getJobs();
        List<CityCostEntry> cities = repository.getCities();
        List<SitemapUrl> urls = new ArrayList<>();

        String today = LocalDate.now().toString();
        String weekAgo = LocalDate.now().minusDays(7).toString();
        String monthAgo = LocalDate.now().minusMonths(1).toString();

        // 1. Static Pages - Highest Priority
        urls.add(new SitemapUrl(comparisonService.buildCanonicalUrl("/"), today, "1.0", "daily"));
        urls.add(new SitemapUrl(comparisonService.buildCanonicalUrl("/cities"), weekAgo, "0.9", "weekly"));
        urls.add(new SitemapUrl(comparisonService.buildCanonicalUrl("/methodology"), monthAgo, "1.0", "monthly"));
        urls.add(new SitemapUrl(comparisonService.buildCanonicalUrl("/about"), monthAgo, "0.7", "monthly"));
        urls.add(new SitemapUrl(comparisonService.buildCanonicalUrl("/privacy"), monthAgo, "0.3", "yearly"));
        urls.add(new SitemapUrl(comparisonService.buildCanonicalUrl("/terms"), monthAgo, "0.3", "yearly"));
        urls.add(new SitemapUrl(comparisonService.buildCanonicalUrl("/contact"), monthAgo, "0.5", "monthly"));

        // 2. Single City Analysis (Salary Check) - [SEO OPTIMIZED STRATEGY]
        // Focus on all cities but with tiered priorities
        List<Integer> salaryBuckets = buildSalaryBuckets();

        for (CityCostEntry city : cities) {
            String priority = "0.6"; // Baseline
            if (city.getTier() == 1)
                priority = "0.8";
            if (city.getTier() >= 3)
                priority = "0.3";

            for (int salary : salaryBuckets) {
                String path = "/salary-check/" + city.getSlug() + "/" + salary;
                urls.add(new SitemapUrl(
                        comparisonService.buildCanonicalUrl(path),
                        weekAgo,
                        priority,
                        "weekly"));
            }
        }

        // 3. Job x CityA x CityB Comparisons - [TIERED PRIORITY STRATEGY]
        for (JobInfo job : jobs) {
            for (CityCostEntry cityA : cities) {
                for (CityCostEntry cityB : cities) {
                    if (!cityA.getSlug().equals(cityB.getSlug())) {

                        // Calculate Tiered Priority
                        // 1.0 = Home, 0.9 = Hot Pairs, 0.7 = Major, 0.3 = Minor
                        double priorityValue = 0.5; // Default

                        boolean isMajorJob = job.isMajor();
                        int combinedTier = cityA.getTier() + cityB.getTier();

                        if (isMajorJob && combinedTier <= 2)
                            priorityValue = 0.9;
                        else if (isMajorJob && combinedTier <= 4)
                            priorityValue = 0.7;
                        else if (!isMajorJob && combinedTier <= 2)
                            priorityValue = 0.6;
                        else if (combinedTier >= 6)
                            priorityValue = 0.1; // Very minor

                        String path = "/" + job.getSlug() + "-salary-" + cityA.getSlug() + "-vs-" + cityB.getSlug();
                        urls.add(new SitemapUrl(
                                comparisonService.buildCanonicalUrl(path),
                                weekAgo,
                                String.valueOf(priorityValue),
                                (priorityValue >= 0.7 ? "weekly" : "monthly")));
                    }
                }
            }
        }

        return urls.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Build SEO-optimized salary buckets with tiered intervals
     * Based on search volume analysis:
     * - $15K-$60K: High search volume (students, entry-level) → $5K intervals
     * - $60K-$120K: Very high search volume (mid-senior) → $10K intervals
     * - $120K-$200K: Moderate search volume (senior-exec) → $20K intervals
     * - Excludes $200K+ (< 0.5% search volume, spam risk)
     */
    private List<Integer> buildSalaryBuckets() {
        List<Integer> buckets = new ArrayList<>();

        // Tier 1: $15K-$60K, $5K intervals (10 buckets)
        // Covers: Students, part-time, entry-level positions
        for (int s = 15000; s <= 60000; s += 5000) {
            buckets.add(s);
        }

        // Tier 2: $60K-$130K, $5K intervals (Crucial Range)
        // Covers: Teachers ($65k), Residents ($68k est using 70k), Nurses ($95k),
        // Managers ($110k), Consultants ($125k)
        // We moved to $5k intervals here because this is the "Golden Zone" of traffic.
        for (int s = 65000; s <= 130000; s += 5000) {
            buckets.add(s);
        }

        // Tier 3: $140K-$250K, $10K intervals
        // Covers: Travel Nurses ($135k+), Tech ($150k), IB ($175k), Doctors ($240k)
        for (int s = 140000; s <= 250000; s += 10000) {
            buckets.add(s);
        }

        // Total: More buckets, but precise targeting for our new "Whale" professions.
        return buckets;
    }

    private ResponseEntity<String> respondWithCache(String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePublic().getHeaderValue())
                .body(body);
    }
}
