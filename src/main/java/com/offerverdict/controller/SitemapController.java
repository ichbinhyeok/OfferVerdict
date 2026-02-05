package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SitemapController {

    private final DataRepository repository;
    private final String BASE_URL = "https://livingcostcheck.com";

    public SitemapController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static Pages
        addUrl(xml, "/", "1.0");
        addUrl(xml, "/about", "0.8");
        addUrl(xml, "/methodology", "0.8");
        addUrl(xml, "/cities", "0.9");

        // Dynamic Seed Strategy: Focus on Tech Hubs & Rising Cities
        // Instead of generating 5000+ thin pages, we focus on high-intent seeds.

        List<CityCostEntry> allCities = repository.getCities();
        List<JobInfo> allJobs = repository.getJobs();

        // 1. Tech Hubs (San Francisco, New York, Seattle, Austin, Boston)
        List<String> targetCitySlugs = List.of("san-francisco-ca", "new-york-ny", "seattle-wa", "austin-tx",
                "boston-ma", "los-angeles-ca", "denver-co", "chicago-il");
        // 2. High Volume Roles
        List<String> targetJobSlugs = List.of("software-engineer", "product-manager", "data-scientist",
                "marketing-manager", "finance", "registered-nurse");

        for (CityCostEntry city : allCities) {
            if (targetCitySlugs.contains(city.getSlug())) {
                for (JobInfo job : allJobs) {
                    if (targetJobSlugs.contains(job.getSlug())) {
                        // Key Salaries Only: Start, Mid, Senior
                        // This reduces bloat and focuses on "Is $100k good?" type intent.
                        int[] salaryPoints = { 80000, 100000, 120000, 150000, 200000 };

                        for (int s : salaryPoints) {
                            addUrl(xml, "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + s, "0.9");
                        }
                    }
                }
            }
        }

        // Add Comparisons Seed
        addUrl(xml, "/software-engineer-salary-san-francisco-ca-vs-austin-tx", "0.8");
        addUrl(xml, "/financial-analyst-salary-new-york-ny-vs-chicago-il", "0.8");

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String path, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(BASE_URL).append(path).append("</loc>\n");
        xml.append("    <changefreq>weekly</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
