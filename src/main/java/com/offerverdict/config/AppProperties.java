package com.offerverdict.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String publicBaseUrl;
    private double baselineLivingCost;
    private int sitemapChunkSize = 25;
    private boolean devReloadEnabled = false;

    // SEO / Salary Bucket Config
    private int seoSalaryBucketInterval = 10000;
    private int seoSalaryBucketMin = 30000;
    private int seoSalaryBucketMax = 500000;

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public double getBaselineLivingCost() {
        return baselineLivingCost;
    }

    public void setBaselineLivingCost(double baselineLivingCost) {
        this.baselineLivingCost = baselineLivingCost;
    }

    public int getSitemapChunkSize() {
        return sitemapChunkSize;
    }

    public void setSitemapChunkSize(int sitemapChunkSize) {
        this.sitemapChunkSize = sitemapChunkSize;
    }

    public boolean isDevReloadEnabled() {
        return devReloadEnabled;
    }

    public void setDevReloadEnabled(boolean devReloadEnabled) {
        this.devReloadEnabled = devReloadEnabled;
    }

    public int getSeoSalaryBucketInterval() {
        return seoSalaryBucketInterval;
    }

    public void setSeoSalaryBucketInterval(int seoSalaryBucketInterval) {
        this.seoSalaryBucketInterval = seoSalaryBucketInterval;
    }

    public int getSeoSalaryBucketMin() {
        return seoSalaryBucketMin;
    }

    public void setSeoSalaryBucketMin(int seoSalaryBucketMin) {
        this.seoSalaryBucketMin = seoSalaryBucketMin;
    }

    public int getSeoSalaryBucketMax() {
        return seoSalaryBucketMax;
    }

    public void setSeoSalaryBucketMax(int seoSalaryBucketMax) {
        this.seoSalaryBucketMax = seoSalaryBucketMax;
    }
}
