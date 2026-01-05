package com.offerverdict.model;

public class CityCostEntry {
    private String city;
    private String state;
    private String slug;
    private double avgRent;
    private double colIndex;

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

    public void setAvgRent(double avgRent) {
        this.avgRent = avgRent;
    }

    public double getColIndex() {
        return colIndex;
    }

    public void setColIndex(double colIndex) {
        this.colIndex = colIndex;
    }
}
