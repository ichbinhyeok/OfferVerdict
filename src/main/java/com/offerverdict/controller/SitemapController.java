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

        // AI Strategy Update: Expand from ~200 seeds back to ~2,000-4,000 URLs.
        // Include a smart mix of Tier 1/2/3 cities + high intent jobs to re-ignite
        // indexing.

        List<CityCostEntry> allCities = repository.getCities();
        List<JobInfo> allJobs = repository.getJobs();

        for (CityCostEntry city : allCities) {
            for (JobInfo job : allJobs) {
                boolean isTopCity = city.getTier() <= 2;
                boolean isTopJob = "software-engineer".equals(job.getSlug()) ||
                        "registered-nurse".equals(job.getSlug()) ||
                        "product-manager".equals(job.getSlug()) ||
                        "financial-analyst".equals(job.getSlug());

                int[] salaryPoints;
                // Dynamically adjust density based on importance
                if (isTopCity && isTopJob) {
                    salaryPoints = new int[] { 60000, 80000, 100000, 120000, 150000, 200000 };
                } else if (isTopCity || isTopJob) {
                    salaryPoints = new int[] { 75000, 100000, 150000 };
                } else {
                    // Tier 3/Long-tail: Anchor around common question "Is 100k good?"
                    salaryPoints = new int[] { 70000, 100000 };
                }

                for (int s : salaryPoints) {
                    addUrl(xml, "/salary-check/" + job.getSlug() + "/" + city.getSlug() + "/" + s, "0.9");
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
