package com.offerverdict.model;

import java.util.Map;

public class AuthoritativeMetrics {
    private Metadata metadata;
    private Map<String, Double> localIncomeTaxes;
    private Map<String, Double> stateCarInsuranceMonthly;
    private HealthInsuranceMetrics healthInsuranceMonthly;
    private Map<String, Double> bigMacIndex;
    private Benchmarks benchmarks;

    public static class Metadata {
        public String source;
        public String lastUpdated;
    }

    public static class HealthInsuranceMetrics {
        public double nationalAverageSingle;
        public Map<String, Double> premiumStateHigh;
        public Map<String, Double> premiumStateLow;

        public double getNationalAverageSingle() {
            return nationalAverageSingle;
        }

        public Map<String, Double> getPremiumStateHigh() {
            return premiumStateHigh;
        }

        public Map<String, Double> getPremiumStateLow() {
            return premiumStateLow;
        }
    }

    public static class Benchmarks {
        public double averageHSAContributionEmployer;
        public double average401kMatchPercent;
        public double typicalCommuteMinutes;

        public double getAverageHSAContributionEmployer() {
            return averageHSAContributionEmployer;
        }

        public double getAverage401kMatchPercent() {
            return average401kMatchPercent;
        }

        public double getTypicalCommuteMinutes() {
            return typicalCommuteMinutes;
        }
    }

    // Getters and Setters
    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Map<String, Double> getLocalIncomeTaxes() {
        return localIncomeTaxes;
    }

    public void setLocalIncomeTaxes(Map<String, Double> localIncomeTaxes) {
        this.localIncomeTaxes = localIncomeTaxes;
    }

    public Map<String, Double> getStateCarInsuranceMonthly() {
        return stateCarInsuranceMonthly;
    }

    public void setStateCarInsuranceMonthly(Map<String, Double> stateCarInsuranceMonthly) {
        this.stateCarInsuranceMonthly = stateCarInsuranceMonthly;
    }

    public HealthInsuranceMetrics getHealthInsuranceMonthly() {
        return healthInsuranceMonthly;
    }

    public void setHealthInsuranceMonthly(HealthInsuranceMetrics healthInsuranceMonthly) {
        this.healthInsuranceMonthly = healthInsuranceMonthly;
    }

    public Map<String, Double> getBigMacIndex() {
        return bigMacIndex;
    }

    public void setBigMacIndex(Map<String, Double> bigMacIndex) {
        this.bigMacIndex = bigMacIndex;
    }

    public Benchmarks getBenchmarks() {
        return benchmarks;
    }

    public void setBenchmarks(Benchmarks benchmarks) {
        this.benchmarks = benchmarks;
    }
}
