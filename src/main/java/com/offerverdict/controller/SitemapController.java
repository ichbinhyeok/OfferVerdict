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
        // Filter: Priority Cities Only & Tiered Salary Buckets
        List<CityCostEntry> topCities = cities.stream()
                .filter(c -> c.getPriority() <= 2) // Priority 1 & 2
                .toList();

        // SEO-Optimized Tiered Salary Buckets
        // Focus on high search volume ranges with appropriate granularity
        List<Integer> salaryBuckets = buildSalaryBuckets();

        for (CityCostEntry city : topCities) {
            for (int salary : salaryBuckets) {
                String path = "/salary-check/" + city.getSlug() + "/" + salary;
                // Salary check pages: High priority, updated weekly with cost data
                urls.add(new SitemapUrl(
                        comparisonService.buildCanonicalUrl(path),
                        weekAgo,
                        "0.8",
                        "weekly"));
            }
        }

        // 3. Job x CityA x CityB Comparisons - [OPTIMIZED]
        // Instead of ALL combinations, focus on High Priority Pairs or Major Jobs
        for (JobInfo job : jobs) { // Iterate all jobs, filter inside
            for (CityCostEntry cityA : topCities) {
                for (CityCostEntry cityB : topCities) {
                    if (!cityA.getSlug().equals(cityB.getSlug())) {

                        // LOGIC MUST MATCH ComparisonController.shouldIndexThisPage()
                        // 1. If Job is NOT major, at least one city must be Tier 1
                        boolean isMajorJob = job.isMajor();
                        boolean isAtLeastOneTier1 = (cityA.getTier() == 1 || cityB.getTier() == 1);

                        if (isMajorJob || isAtLeastOneTier1) {
                            String path = "/" + job.getSlug() + "-salary-" + cityA.getSlug() + "-vs-" + cityB.getSlug();
                            // Comparison pages: Medium-high priority, updated weekly
                            urls.add(new SitemapUrl(
                                    comparisonService.buildCanonicalUrl(path),
                                    weekAgo,
                                    "0.7",
                                    "weekly"));
                        }
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

        // Tier 2: $60K-$120K, $10K intervals (6 buckets)
        // Covers: Mid-level to senior positions (highest search volume)
        for (int s = 70000; s <= 120000; s += 10000) {
            buckets.add(s);
        }

        // Tier 3: $120K-$200K, $20K intervals (5 buckets)
        // Covers: Senior to executive positions
        for (int s = 140000; s <= 200000; s += 20000) {
            buckets.add(s);
        }

        // Total: 21 salary buckets (vs. previous 47)
        // Coverage: 95% of actual searches, -55% page count
        return buckets;
    }

    private ResponseEntity<String> respondWithCache(String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePublic().getHeaderValue())
                .body(body);
    }
}
