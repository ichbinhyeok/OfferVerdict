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

    // Decision-model assumptions (tunable without code changes)
    private double authorityYearlyGainThreshold = 40000;
    private double relocationBaselineSalarySf = 180000;
    private double relocationBaselineSalaryNyc = 170000;
    private double carAffordabilityTarget = 50000;
    private double commuteCostPerMinute = 5;
    private boolean enforceCanonicalHostRedirect = true;
    private boolean enforceCanonicalSchemeRedirect = false;

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

    public double getAuthorityYearlyGainThreshold() {
        return authorityYearlyGainThreshold;
    }

    public void setAuthorityYearlyGainThreshold(double authorityYearlyGainThreshold) {
        this.authorityYearlyGainThreshold = authorityYearlyGainThreshold;
    }

    public double getRelocationBaselineSalarySf() {
        return relocationBaselineSalarySf;
    }

    public void setRelocationBaselineSalarySf(double relocationBaselineSalarySf) {
        this.relocationBaselineSalarySf = relocationBaselineSalarySf;
    }

    public double getRelocationBaselineSalaryNyc() {
        return relocationBaselineSalaryNyc;
    }

    public void setRelocationBaselineSalaryNyc(double relocationBaselineSalaryNyc) {
        this.relocationBaselineSalaryNyc = relocationBaselineSalaryNyc;
    }

    public double getCarAffordabilityTarget() {
        return carAffordabilityTarget;
    }

    public void setCarAffordabilityTarget(double carAffordabilityTarget) {
        this.carAffordabilityTarget = carAffordabilityTarget;
    }

    public double getCommuteCostPerMinute() {
        return commuteCostPerMinute;
    }

    public void setCommuteCostPerMinute(double commuteCostPerMinute) {
        this.commuteCostPerMinute = commuteCostPerMinute;
    }

    public boolean isEnforceCanonicalHostRedirect() {
        return enforceCanonicalHostRedirect;
    }

    public void setEnforceCanonicalHostRedirect(boolean enforceCanonicalHostRedirect) {
        this.enforceCanonicalHostRedirect = enforceCanonicalHostRedirect;
    }

    public boolean isEnforceCanonicalSchemeRedirect() {
        return enforceCanonicalSchemeRedirect;
    }

    public void setEnforceCanonicalSchemeRedirect(boolean enforceCanonicalSchemeRedirect) {
        this.enforceCanonicalSchemeRedirect = enforceCanonicalSchemeRedirect;
    }
}
