package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SitemapControllerTest {

    @Test
    void sitemapOnlyEmitsV2IndexablePaths() {
        AppProperties appProperties = new AppProperties();
        appProperties.setPublicBaseUrl("https://livingcostcheck.com/");

        SitemapController controller = new SitemapController(appProperties);
        String xml = controller.sitemap();

        assertTrue(xml.contains("/nurse-relocation-offer-checker</loc>"));
        assertTrue(xml.contains("/sign-on-bonus-repayment-calculator</loc>"));
        assertTrue(xml.contains("/shift-differential-calculator</loc>"));
        assertTrue(xml.contains("/rn-sign-on-bonus-clawback</loc>"));
        assertTrue(xml.contains("/rn-offer-red-flags</loc>"));
        assertTrue(xml.contains("/should-i-accept-nurse-job-offer</loc>"));
        assertTrue(xml.contains("/nurse-offer-letter-red-flags</loc>"));
        assertTrue(xml.contains("/nurse-offer-negotiation-questions</loc>"));
        assertTrue(xml.contains("/compare-two-nurse-job-offers</loc>"));
        assertTrue(xml.contains("/new-grad-nurse-offer-red-flags</loc>"));
        assertTrue(xml.contains("/travel-nurse-contract-red-flags</loc>"));
        assertTrue(xml.contains("/rn-float-policy-offer</loc>"));
        assertTrue(xml.contains("/rn-low-census-cancellation</loc>"));
        assertTrue(xml.contains("/icu-nurse-offer-red-flags</loc>"));
        assertTrue(xml.contains("/ed-nurse-offer-red-flags</loc>"));
        assertTrue(xml.contains("/labor-delivery-nurse-offer-red-flags</loc>"));
        assertTrue(xml.contains("/icu-rn-guaranteed-hours</loc>"));
        assertTrue(xml.contains("/ed-rn-float-policy</loc>"));
        assertTrue(xml.contains("/med-surg-tele-rn-staffing-ratio</loc>"));
        assertTrue(xml.contains("/labor-delivery-rn-orientation-length</loc>"));
        assertTrue(xml.contains("/home-health-rn-weekend-holiday-requirement</loc>"));
        assertTrue(countOccurrences(xml, "<url>") >= 180);
        assertFalse(xml.contains("/salary-check/"));
        assertFalse(xml.contains("/job/"));
        assertFalse(xml.contains("/city/"));
    }

    @Test
    void sitemapUsesNormalizedBaseUrlWithoutDoubleSlash() {
        AppProperties appProperties = new AppProperties();
        appProperties.setPublicBaseUrl("https://livingcostcheck.com/");

        SitemapController controller = new SitemapController(appProperties);
        String xml = controller.sitemap();

        assertTrue(xml.contains("<loc>https://livingcostcheck.com/</loc>"));
        assertFalse(xml.contains("https://livingcostcheck.com//"));
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
