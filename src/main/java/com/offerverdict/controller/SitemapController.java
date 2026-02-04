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
    private final String BASE_URL = "https://offerverdict.com"; // Change to actual domain

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
        addUrl(xml, "/cities", "0.9"); // Directory root

        // Dynamic Pages: Top 50 Cities x Top Jobs
        // Strategy: We don't want 10M pages yet. Pick Top Cities and Top Jobs.
        List<CityCostEntry> allCities = repository.getCities();
        List<JobInfo> allJobs = repository.getJobs();

        // Filter for "Popular" logic (simplified: first 20 cities/jobs for now)
        // In real app, we would have a 'population' field or 'isPopular' flag.
        int cityLimit = Math.min(allCities.size(), 30);
        int jobLimit = Math.min(allJobs.size(), 10);

        for (int i = 0; i < cityLimit; i++) {
            CityCostEntry city = allCities.get(i);

            for (int j = 0; j < jobLimit; j++) {
                JobInfo job = allJobs.get(j);

                // Tiered Salary Logic: $20k to $250k
                // Tier 1: 20-60 ($10k steps)
                for (int s = 20000; s <= 60000; s += 10000) {
                    addUrl(xml, "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + s, "0.6");
                }
                // Tier 2: 80-150 ($10k steps)
                for (int s = 80000; s <= 150000; s += 10000) {
                    addUrl(xml, "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + s, "0.7");
                }
                // Tier 3: 175-250 ($25k steps)
                for (int s = 175000; s <= 250000; s += 25000) {
                    addUrl(xml, "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + s, "0.5");
                }
            }
        }

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
