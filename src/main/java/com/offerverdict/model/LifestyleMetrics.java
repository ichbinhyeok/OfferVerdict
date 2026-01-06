package com.offerverdict.model;

public class LifestyleMetrics {
    private double starbucksCoffees;
    private double netflixSubscriptions;
    private double uberEatsOrders;
    private double weekendGetaways;
    private double freedomHours;
    private double hourlyRate;

    public LifestyleMetrics() {
    }

    public LifestyleMetrics(double residual, double hourlyRate) {
        this.hourlyRate = hourlyRate;
        this.starbucksCoffees = residual / 6.50;
        this.netflixSubscriptions = residual / 15.99;
        this.uberEatsOrders = residual / 25.0;
        this.weekendGetaways = residual / 300.0;
        this.freedomHours = hourlyRate > 0 ? residual / hourlyRate : 0;
    }

    public double getStarbucksCoffees() {
        return starbucksCoffees;
    }

    public void setStarbucksCoffees(double starbucksCoffees) {
        this.starbucksCoffees = starbucksCoffees;
    }

    public double getNetflixSubscriptions() {
        return netflixSubscriptions;
    }

    public void setNetflixSubscriptions(double netflixSubscriptions) {
        this.netflixSubscriptions = netflixSubscriptions;
    }

    public double getUberEatsOrders() {
        return uberEatsOrders;
    }

    public void setUberEatsOrders(double uberEatsOrders) {
        this.uberEatsOrders = uberEatsOrders;
    }

    public double getWeekendGetaways() {
        return weekendGetaways;
    }

    public void setWeekendGetaways(double weekendGetaways) {
        this.weekendGetaways = weekendGetaways;
    }

    public double getFreedomHours() {
        return freedomHours;
    }

    public void setFreedomHours(double freedomHours) {
        this.freedomHours = freedomHours;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
}

