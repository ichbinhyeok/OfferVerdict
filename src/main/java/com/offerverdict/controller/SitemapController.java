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

    public SitemapController(DataRepository repository, ComparisonService comparisonService, AppProperties appProperties) {
        this.repository = repository;
        this.comparisonService = comparisonService;
        this.appProperties = appProperties;
    }

    @GetMapping(value = "/sitemap-index.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemapIndex() {
        List<String> urls = buildComparisonUrls();
        int chunkSize = appProperties.getSitemapChunkSize();
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
        List<String> urls = buildComparisonUrls();
        int chunkSize = appProperties.getSitemapChunkSize();
        int from = Math.max(0, (page - 1) * chunkSize);
        if (from >= urls.size()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
        int to = Math.min(urls.size(), from + chunkSize);
        List<String> slice = urls.subList(from, to);

        String today = LocalDate.now().toString();
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        for (String url : slice) {
            xml.append("<url><loc>").append(url).append("</loc><lastmod>").append(today).append("</lastmod></url>");
        }
        xml.append("</urlset>");
        return respondWithCache(xml.toString());
    }

    private List<String> buildComparisonUrls() {
        List<JobInfo> jobs = repository.getJobs();
        List<CityCostEntry> cities = repository.getCities();
        List<String> paths = new ArrayList<>();
        for (JobInfo job : jobs) {
            for (CityCostEntry cityA : cities) {
                for (CityCostEntry cityB : cities) {
                    if (!cityA.getSlug().equals(cityB.getSlug())) {
                        String path = "/" + job.getSlug() + "-salary-" + cityA.getSlug() + "-vs-" + cityB.getSlug()
                                + "?currentSalary=120000&offerSalary=150000";
                        paths.add(comparisonService.buildCanonicalUrl(path));
                    }
                }
            }
        }
        return paths.stream().distinct().collect(Collectors.toList());
    }

    private ResponseEntity<String> respondWithCache(String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePublic().getHeaderValue())
                .body(body);
    }
}
