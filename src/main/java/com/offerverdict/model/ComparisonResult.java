package com.offerverdict.model;

public class ComparisonResult {
    private Verdict verdict;
    private String verdictCopy;
    private double deltaPercent;
    private double maxResidual;
    private LifestyleMetrics currentLifestyle;
    private LifestyleMetrics offerLifestyle;
    private ComparisonBreakdown current;
    private ComparisonBreakdown offer;

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public String getVerdictCopy() {
        return verdictCopy;
    }

    public void setVerdictCopy(String verdictCopy) {
        this.verdictCopy = verdictCopy;
    }

    public double getDeltaPercent() {
        return deltaPercent;
    }

    public void setDeltaPercent(double deltaPercent) {
        this.deltaPercent = deltaPercent;
    }

    public double getMaxResidual() {
        return maxResidual;
    }

    public void setMaxResidual(double maxResidual) {
        this.maxResidual = maxResidual;
    }

    public LifestyleMetrics getCurrentLifestyle() {
        return currentLifestyle;
    }

    public void setCurrentLifestyle(LifestyleMetrics currentLifestyle) {
        this.currentLifestyle = currentLifestyle;
    }

    public LifestyleMetrics getOfferLifestyle() {
        return offerLifestyle;
    }

    public void setOfferLifestyle(LifestyleMetrics offerLifestyle) {
        this.offerLifestyle = offerLifestyle;
    }

    public ComparisonBreakdown getCurrent() {
        return current;
    }

    public void setCurrent(ComparisonBreakdown current) {
        this.current = current;
    }

    public ComparisonBreakdown getOffer() {
        return offer;
    }

    public void setOffer(ComparisonBreakdown offer) {
        this.offer = offer;
    }
}
