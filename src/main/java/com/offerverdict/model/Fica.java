package com.offerverdict.model;

public class Fica {
    private double socialSecurityRate;
    private double socialSecurityCap;
    private double medicareRate;

    public double getSocialSecurityRate() {
        return socialSecurityRate;
    }

    public void setSocialSecurityRate(double socialSecurityRate) {
        this.socialSecurityRate = socialSecurityRate;
    }

    public double getSocialSecurityCap() {
        return socialSecurityCap;
    }

    public void setSocialSecurityCap(double socialSecurityCap) {
        this.socialSecurityCap = socialSecurityCap;
    }

    public double getMedicareRate() {
        return medicareRate;
    }

    public void setMedicareRate(double medicareRate) {
        this.medicareRate = medicareRate;
    }
}
