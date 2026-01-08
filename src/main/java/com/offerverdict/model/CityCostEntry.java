package com.offerverdict.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class CityCostEntry {
    private String city;
    private String state;
    private String slug;
    private double avgRent;
    private double colIndex;
    private double medianIncome;
    private double avgHousePrice;
    private Map<String, Double> details;
    private Map<String, Double> lifestyle;

    // SEO Filtering Fields
    private int priority = 99; // Default low priority
    private int tier = 3;      // Default Tier 3 (standard)

    public CityCostEntry() {
    }

    public CityCostEntry(String city, String state, String slug, double avgRent, double colIndex, double medianIncome, double avgHousePrice, Map<String, Double> details) {
        this.city = city;
        this.state = state;
        this.slug = slug;
        this.avgRent = avgRent;
        this.colIndex = colIndex;
        this.medianIncome = medianIncome;
        this.avgHousePrice = avgHousePrice;
        this.details = details;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public double getAvgRent() {
        return avgRent;
    }

    @JsonProperty("avgRent")
    public void setAvgRent(double avgRent) {
        this.avgRent = avgRent;
    }

    public double getColIndex() {
        return colIndex;
    }

    @JsonProperty("colIndex")
    public void setColIndex(double colIndex) {
        this.colIndex = colIndex;
    }

    public double getMedianIncome() {
        return medianIncome;
    }

    @JsonProperty("medianIncome")
    public void setMedianIncome(double medianIncome) {
        this.medianIncome = medianIncome;
    }

    public double getAvgHousePrice() {
        return avgHousePrice;
    }

    @JsonProperty("avgHousePrice")
    public void setAvgHousePrice(double avgHousePrice) {
        this.avgHousePrice = avgHousePrice;
    }

    public Map<String, Double> getDetails() {
        return details;
    }

    @JsonProperty("details")
    public void setDetails(Map<String, Double> details) {
        this.details = details;
    }

    public Map<String, Double> getLifestyle() {
        return lifestyle;
    }

    @JsonProperty("lifestyle")
    public void setLifestyle(Map<String, Double> lifestyle) {
        this.lifestyle = lifestyle;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }
}
