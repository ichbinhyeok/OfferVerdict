package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
public class SitemapController {
    private static final List<String> CORE_INDEXABLE_PATHS = List.of(
            "/",
            "/nurse-relocation-offer-checker",
            "/sign-on-bonus-repayment-calculator",
            "/shift-differential-calculator",
            "/methodology",
            "/about");
    private static final List<String> V2_INDEXABLE_PATHS = indexablePaths();

    private final AppProperties appProperties;

    public SitemapController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    private static List<String> indexablePaths() {
        List<String> paths = new ArrayList<>(CORE_INDEXABLE_PATHS);
        paths.addAll(NurseOfferIssueController.INDEXABLE_PATHS);
        return List.copyOf(paths);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (String path : V2_INDEXABLE_PATHS) {
            addUrl(xml, path, "/".equals(path) ? "1.0" : "0.9");
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
}
