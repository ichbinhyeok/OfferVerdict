package com.offerverdict.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxDefaults {
    @JsonProperty("max_401k_contribution")
    private double max401kContribution;

    @JsonProperty("standard_monthly_insurance")
    private double standardMonthlyInsurance;

    @JsonProperty("rsu_supplemental_rate")
    private double rsuSupplementalRate;

    @JsonProperty("default_car_insurance_monthly")
    private double defaultCarInsuranceMonthly;

    public double getMax401kContribution() {
        return max401kContribution;
    }

    public void setMax401kContribution(double max401kContribution) {
        this.max401kContribution = max401kContribution;
    }

    public double getStandardMonthlyInsurance() {
        return standardMonthlyInsurance;
    }

    public void setStandardMonthlyInsurance(double standardMonthlyInsurance) {
        this.standardMonthlyInsurance = standardMonthlyInsurance;
    }

    public double getRsuSupplementalRate() {
        return rsuSupplementalRate;
    }

    public void setRsuSupplementalRate(double rsuSupplementalRate) {
        this.rsuSupplementalRate = rsuSupplementalRate;
    }

    public double getDefaultCarInsuranceMonthly() {
        return defaultCarInsuranceMonthly;
    }

    public void setDefaultCarInsuranceMonthly(double defaultCarInsuranceMonthly) {
        this.defaultCarInsuranceMonthly = defaultCarInsuranceMonthly;
    }
}
