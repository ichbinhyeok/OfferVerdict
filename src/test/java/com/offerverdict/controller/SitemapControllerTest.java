package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SitemapControllerTest {

    @Test
    void sitemapAlignsSalaryPointsWithSeoInterval() {
        DataRepository repository = mock(DataRepository.class);

        CityCostEntry topCity = new CityCostEntry();
        topCity.setSlug("austin-tx");
        topCity.setTier(1);

        CityCostEntry standardCity = new CityCostEntry();
        standardCity.setSlug("omaha-ne");
        standardCity.setTier(3);

        JobInfo topJob = new JobInfo();
        topJob.setSlug("software-engineer");

        JobInfo longTailJob = new JobInfo();
        longTailJob.setSlug("teacher");

        when(repository.getCities()).thenReturn(List.of(topCity, standardCity));
        when(repository.getJobs()).thenReturn(List.of(topJob, longTailJob));

        AppProperties appProperties = new AppProperties();
        appProperties.setPublicBaseUrl("https://livingcostcheck.com/");
        appProperties.setSeoSalaryBucketInterval(10_000);

        SitemapController controller = new SitemapController(repository, appProperties);
        String xml = controller.sitemap();

        // 75,000 would 301 to 80,000 in SingleCityController; sitemap must never emit it.
        assertFalse(xml.contains("/75000</loc>"));
        assertTrue(xml.contains("/salary-check/teacher/austin-tx/80000</loc>"));

        Pattern pattern = Pattern.compile("/salary-check/[^/]+/[^/]+/(\\d+)</loc>");
        Matcher matcher = pattern.matcher(xml);
        while (matcher.find()) {
            int salary = Integer.parseInt(matcher.group(1));
            assertTrue(salary % 10_000 == 0, "Salary in sitemap is not aligned: " + salary);
        }
    }

    @Test
    void sitemapUsesNormalizedBaseUrlWithoutDoubleSlash() {
        DataRepository repository = mock(DataRepository.class);
        when(repository.getCities()).thenReturn(List.of());
        when(repository.getJobs()).thenReturn(List.of());

        AppProperties appProperties = new AppProperties();
        appProperties.setPublicBaseUrl("https://livingcostcheck.com/");
        appProperties.setSeoSalaryBucketInterval(10_000);

        SitemapController controller = new SitemapController(repository, appProperties);
        String xml = controller.sitemap();

        assertTrue(xml.contains("<loc>https://livingcostcheck.com/</loc>"));
        assertFalse(xml.contains("https://livingcostcheck.com//"));
    }
}
