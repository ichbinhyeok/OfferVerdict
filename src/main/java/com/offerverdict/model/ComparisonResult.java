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

    // Receipt UI Fields
    private String monthlyGainStr;
    private String freedomIndex; // e.g. "25%"
    private String taxDiffMsg;
    private String rentDiffMsg;
    private String valueDiffMsg;

    // Negotiation Leverage
    private double breakEvenSalary;
    private String leverageMsg; // "Ask for $145,000 to match your current lifestyle."

    private String jobPercentile; // e.g. "22%"
    private boolean isGoodSalary;

    // Getters and Setters
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

    public String getMonthlyGainStr() {
        return monthlyGainStr;
    }

    public void setMonthlyGainStr(String monthlyGainStr) {
        this.monthlyGainStr = monthlyGainStr;
    }

    public String getFreedomIndex() {
        return freedomIndex;
    }

    public void setFreedomIndex(String freedomIndex) {
        this.freedomIndex = freedomIndex;
    }

    public String getTaxDiffMsg() {
        return taxDiffMsg;
    }

    public void setTaxDiffMsg(String taxDiffMsg) {
        this.taxDiffMsg = taxDiffMsg;
    }

    public String getRentDiffMsg() {
        return rentDiffMsg;
    }

    public void setRentDiffMsg(String rentDiffMsg) {
        this.rentDiffMsg = rentDiffMsg;
    }

    public String getValueDiffMsg() {
        return valueDiffMsg;
    }

    public void setValueDiffMsg(String valueDiffMsg) {
        this.valueDiffMsg = valueDiffMsg;
    }

    public String getJobPercentile() {
        return jobPercentile;
    }

    public void setJobPercentile(String jobPercentile) {
        this.jobPercentile = jobPercentile;
    }

    public boolean isGoodSalary() {
        return isGoodSalary;
    }

    public void setGoodSalary(boolean goodSalary) {
        isGoodSalary = goodSalary;
    }

    public double getBreakEvenSalary() {
        return breakEvenSalary;
    }

    public void setBreakEvenSalary(double breakEvenSalary) {
        this.breakEvenSalary = breakEvenSalary;
    }

    public String getLeverageMsg() {
        return leverageMsg;
    }

    public void setLeverageMsg(String leverageMsg) {
        this.leverageMsg = leverageMsg;
    }
}
