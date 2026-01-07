package com.offerverdict.model;

import com.offerverdict.service.TaxCalculatorService;

public class ComparisonBreakdown {
    private double grossSalary; // Added for reverse calc
    private double netMonthly;
    private double rent;
    private double livingCost;
    private double residual;
    private double groceries;
    private double transport;
    private double utilities;
    private double misc;
    private double yearsToBuyHouse;
    private double monthsToBuyTesla;
    private TaxCalculatorService.TaxResult taxResult;

    public double getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(double grossSalary) {
        this.grossSalary = grossSalary;
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
}
