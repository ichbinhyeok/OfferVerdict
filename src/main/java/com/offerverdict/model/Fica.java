package com.offerverdict.model;

public class Fica {
    private double socialSecurityRate;
    private double socialSecurityCap;
    private double medicareRate;
    private double additionalMedicareRate;
    private double additionalMedicareThresholdSingle;
    private double additionalMedicareThresholdMarried;

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

    public double getAdditionalMedicareRate() {
        return additionalMedicareRate;
    }

    public void setAdditionalMedicareRate(double additionalMedicareRate) {
        this.additionalMedicareRate = additionalMedicareRate;
    }

    public double getAdditionalMedicareThresholdSingle() {
        return additionalMedicareThresholdSingle;
    }

    public void setAdditionalMedicareThresholdSingle(double additionalMedicareThresholdSingle) {
        this.additionalMedicareThresholdSingle = additionalMedicareThresholdSingle;
    }

    public double getAdditionalMedicareThresholdMarried() {
        return additionalMedicareThresholdMarried;
    }

    public void setAdditionalMedicareThresholdMarried(double additionalMedicareThresholdMarried) {
        this.additionalMedicareThresholdMarried = additionalMedicareThresholdMarried;
    }
}
