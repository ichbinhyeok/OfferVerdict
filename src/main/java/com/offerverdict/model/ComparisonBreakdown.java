package com.offerverdict.model;

import com.offerverdict.service.TaxCalculatorService;

public class ComparisonBreakdown {
    private double grossSalary; // Added for reverse calc
    private String cityName;
    private double netMonthly;
    private double rent;
    private double livingCost;
    private double residual;
    private double groceries;
    private double transport;
    private double utilities;
    private double misc;
    private double localTax;
    private double insurance;
    private double yearsToBuyHouse;
    private double monthsToBuyTesla;
    private double starbucksSavings;
    private TaxCalculatorService.TaxResult taxResult;

    // Advanced Lab Fields
    private double equityValue;
    private double signingBonus;
    private double commuteTime; // in minutes
    private double realHourlyRate;

    public double getEquityValue() {
        return equityValue;
    }

    public void setEquityValue(double equityValue) {
        this.equityValue = equityValue;
    }

    public double getSigningBonus() {
        return signingBonus;
    }

    public void setSigningBonus(double signingBonus) {
        this.signingBonus = signingBonus;
    }

    public double getCommuteTime() {
        return commuteTime;
    }

    public void setCommuteTime(double commuteTime) {
        this.commuteTime = commuteTime;
    }

    public double getRealHourlyRate() {
        return realHourlyRate;
    }

    public void setRealHourlyRate(double realHourlyRate) {
        this.realHourlyRate = realHourlyRate;
    }
    
    // Leaks
    private double extraLeaks;

    public double getExtraLeaks() {
        return extraLeaks;
    }

    public void setExtraLeaks(double extraLeaks) {
        this.extraLeaks = extraLeaks;
    }

    // Delta fields for inline badges (Comparison with the other side)
    private double salaryDiff;
    private double taxDiff;
    private double housingDiff;
    private double residualDiff;

    public double getSalaryDiff() {
        return salaryDiff;
    }

    public void setSalaryDiff(double salaryDiff) {
        this.salaryDiff = salaryDiff;
    }

    public double getTaxDiff() {
        return taxDiff;
    }

    public void setTaxDiff(double taxDiff) {
        this.taxDiff = taxDiff;
    }

    public double getHousingDiff() {
        return housingDiff;
    }

    public void setHousingDiff(double housingDiff) {
        this.housingDiff = housingDiff;
    }

    public double getResidualDiff() {
        return residualDiff;
    }

    public void setResidualDiff(double residualDiff) {
        this.residualDiff = residualDiff;
    }

    public double getStarbucksSavings() {
        return starbucksSavings;
    }

    public void setStarbucksSavings(double starbucksSavings) {
        this.starbucksSavings = starbucksSavings;
    }

    public double getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(double grossSalary) {
        this.grossSalary = grossSalary;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public double getNetMonthly() {
        return netMonthly;
    }

    public void setNetMonthly(double netMonthly) {
        this.netMonthly = netMonthly;
    }

    public double getRent() {
        return rent;
    }

    public void setRent(double rent) {
        this.rent = rent;
    }

    public double getLivingCost() {
        return livingCost;
    }

    public void setLivingCost(double livingCost) {
        this.livingCost = livingCost;
    }

    public double getResidual() {
        return residual;
    }

    public void setResidual(double residual) {
        this.residual = residual;
    }

    public double getGroceries() {
        return groceries;
    }

    public void setGroceries(double groceries) {
        this.groceries = groceries;
    }

    public double getTransport() {
        return transport;
    }

    public void setTransport(double transport) {
        this.transport = transport;
    }

    public double getUtilities() {
        return utilities;
    }

    public void setUtilities(double utilities) {
        this.utilities = utilities;
    }

    public double getMisc() {
        return misc;
    }

    public void setMisc(double misc) {
        this.misc = misc;
    }

    public double getYearsToBuyHouse() {
        return yearsToBuyHouse;
    }

    public void setYearsToBuyHouse(double yearsToBuyHouse) {
        this.yearsToBuyHouse = yearsToBuyHouse;
    }

    public double getMonthsToBuyTesla() {
        return monthsToBuyTesla;
    }

    public void setMonthsToBuyTesla(double monthsToBuyTesla) {
        this.monthsToBuyTesla = monthsToBuyTesla;
    }

    public TaxCalculatorService.TaxResult getTaxResult() {
        return taxResult;
    }

    public void setTaxResult(TaxCalculatorService.TaxResult taxResult) {
        this.taxResult = taxResult;
    }

    public double getLocalTax() {
        return localTax;
    }

    public void setLocalTax(double localTax) {
        this.localTax = localTax;
    }

    public double getInsurance() {
        return insurance;
    }

    public void setInsurance(double insurance) {
        this.insurance = insurance;
    }
}
