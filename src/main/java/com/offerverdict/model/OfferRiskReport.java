package com.offerverdict.model;

import java.util.List;

public class OfferRiskReport {
    private String analysisMode;
    private String analysisModeLabel;
    private String roleLabel;
    private String currentCityName;
    private String offerCityName;
    private String verdict;
    private String verdictTone;
    private String verdictSummary;
    private String decisionLockLabel;
    private String actionLabel;
    private String actionDraft;
    private String confidenceLabel;
    private String confidenceSummary;

    private double currentAnnualPay;
    private double offerAnnualPay;
    private double baseAnnualPay;
    private double overtimeAnnualPay;
    private double differentialAnnualPay;
    private double currentMonthlyResidual;
    private double offerMonthlyResidual;
    private double monthlyResidualDelta;
    private double currentMonthlyInsurance;
    private double offerMonthlyInsurance;
    private double monthlyInsuranceDelta;

    private double signOnBonus;
    private double relocationStipend;
    private double movingCost;
    private double estimatedBonusTaxRate;
    private double estimatedNetUpfrontValue;
    private double relocationCoverageGap;
    private int contractMonths;
    private int plannedStayMonths;
    private String repaymentStyle;
    private double repaymentExposure;
    private double breakEvenMonths;
    private double offerPercentileAnchor;
    private String marketAnchorLabel;
    private String unitTypeLabel;
    private String shiftGuaranteeLabel;
    private String floatRiskLabel;
    private String cancelRiskLabel;
    private int nurseScheduleRiskScore;
    private int lifeFitRiskScore;
    private String lifeFitLabel;
    private String lifeFitSummary;
    private List<String> lifeFitSignals;

    private List<String> redFlags;
    private List<String> hrQuestions;
    private List<String> negotiationMoves;
    private List<String> verdictReasons;
    private List<String> decisionLocks;
    private List<String> swingFactors;
    private String packetSummary;
    private List<String> topRisks;
    private String survivabilityHeadline;
    private String survivabilitySummary;
    private List<String> survivabilitySignals;
    private List<String> mustAskNow;
    private String walkAwayLine;

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = analysisMode;
    }

    public String getAnalysisModeLabel() {
        return analysisModeLabel;
    }

    public void setAnalysisModeLabel(String analysisModeLabel) {
        this.analysisModeLabel = analysisModeLabel;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public String getCurrentCityName() {
        return currentCityName;
    }

    public void setCurrentCityName(String currentCityName) {
        this.currentCityName = currentCityName;
    }

    public String getOfferCityName() {
        return offerCityName;
    }

    public void setOfferCityName(String offerCityName) {
        this.offerCityName = offerCityName;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getVerdictTone() {
        return verdictTone;
    }

    public void setVerdictTone(String verdictTone) {
        this.verdictTone = verdictTone;
    }

    public String getVerdictSummary() {
        return verdictSummary;
    }

    public void setVerdictSummary(String verdictSummary) {
        this.verdictSummary = verdictSummary;
    }

    public String getDecisionLockLabel() {
        return decisionLockLabel;
    }

    public void setDecisionLockLabel(String decisionLockLabel) {
        this.decisionLockLabel = decisionLockLabel;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public String getActionDraft() {
        return actionDraft;
    }

    public void setActionDraft(String actionDraft) {
        this.actionDraft = actionDraft;
    }

    public String getConfidenceLabel() {
        return confidenceLabel;
    }

    public void setConfidenceLabel(String confidenceLabel) {
        this.confidenceLabel = confidenceLabel;
    }

    public String getConfidenceSummary() {
        return confidenceSummary;
    }

    public void setConfidenceSummary(String confidenceSummary) {
        this.confidenceSummary = confidenceSummary;
    }

    public double getCurrentAnnualPay() {
        return currentAnnualPay;
    }

    public void setCurrentAnnualPay(double currentAnnualPay) {
        this.currentAnnualPay = currentAnnualPay;
    }

    public double getOfferAnnualPay() {
        return offerAnnualPay;
    }

    public void setOfferAnnualPay(double offerAnnualPay) {
        this.offerAnnualPay = offerAnnualPay;
    }

    public double getBaseAnnualPay() {
        return baseAnnualPay;
    }

    public void setBaseAnnualPay(double baseAnnualPay) {
        this.baseAnnualPay = baseAnnualPay;
    }

    public double getOvertimeAnnualPay() {
        return overtimeAnnualPay;
    }

    public void setOvertimeAnnualPay(double overtimeAnnualPay) {
        this.overtimeAnnualPay = overtimeAnnualPay;
    }

    public double getDifferentialAnnualPay() {
        return differentialAnnualPay;
    }

    public void setDifferentialAnnualPay(double differentialAnnualPay) {
        this.differentialAnnualPay = differentialAnnualPay;
    }

    public double getCurrentMonthlyResidual() {
        return currentMonthlyResidual;
    }

    public void setCurrentMonthlyResidual(double currentMonthlyResidual) {
        this.currentMonthlyResidual = currentMonthlyResidual;
    }

    public double getOfferMonthlyResidual() {
        return offerMonthlyResidual;
    }

    public void setOfferMonthlyResidual(double offerMonthlyResidual) {
        this.offerMonthlyResidual = offerMonthlyResidual;
    }

    public double getMonthlyResidualDelta() {
        return monthlyResidualDelta;
    }

    public void setMonthlyResidualDelta(double monthlyResidualDelta) {
        this.monthlyResidualDelta = monthlyResidualDelta;
    }

    public double getCurrentMonthlyInsurance() {
        return currentMonthlyInsurance;
    }

    public void setCurrentMonthlyInsurance(double currentMonthlyInsurance) {
        this.currentMonthlyInsurance = currentMonthlyInsurance;
    }

    public double getOfferMonthlyInsurance() {
        return offerMonthlyInsurance;
    }

    public void setOfferMonthlyInsurance(double offerMonthlyInsurance) {
        this.offerMonthlyInsurance = offerMonthlyInsurance;
    }

    public double getMonthlyInsuranceDelta() {
        return monthlyInsuranceDelta;
    }

    public void setMonthlyInsuranceDelta(double monthlyInsuranceDelta) {
        this.monthlyInsuranceDelta = monthlyInsuranceDelta;
    }

    public double getSignOnBonus() {
        return signOnBonus;
    }

    public void setSignOnBonus(double signOnBonus) {
        this.signOnBonus = signOnBonus;
    }

    public double getRelocationStipend() {
        return relocationStipend;
    }

    public void setRelocationStipend(double relocationStipend) {
        this.relocationStipend = relocationStipend;
    }

    public double getMovingCost() {
        return movingCost;
    }

    public void setMovingCost(double movingCost) {
        this.movingCost = movingCost;
    }

    public double getEstimatedBonusTaxRate() {
        return estimatedBonusTaxRate;
    }

    public void setEstimatedBonusTaxRate(double estimatedBonusTaxRate) {
        this.estimatedBonusTaxRate = estimatedBonusTaxRate;
    }

    public double getEstimatedNetUpfrontValue() {
        return estimatedNetUpfrontValue;
    }

    public void setEstimatedNetUpfrontValue(double estimatedNetUpfrontValue) {
        this.estimatedNetUpfrontValue = estimatedNetUpfrontValue;
    }

    public double getRelocationCoverageGap() {
        return relocationCoverageGap;
    }

    public void setRelocationCoverageGap(double relocationCoverageGap) {
        this.relocationCoverageGap = relocationCoverageGap;
    }

    public int getContractMonths() {
        return contractMonths;
    }

    public void setContractMonths(int contractMonths) {
        this.contractMonths = contractMonths;
    }

    public int getPlannedStayMonths() {
        return plannedStayMonths;
    }

    public void setPlannedStayMonths(int plannedStayMonths) {
        this.plannedStayMonths = plannedStayMonths;
    }

    public String getRepaymentStyle() {
        return repaymentStyle;
    }

    public void setRepaymentStyle(String repaymentStyle) {
        this.repaymentStyle = repaymentStyle;
    }

    public double getRepaymentExposure() {
        return repaymentExposure;
    }

    public void setRepaymentExposure(double repaymentExposure) {
        this.repaymentExposure = repaymentExposure;
    }

    public double getBreakEvenMonths() {
        return breakEvenMonths;
    }

    public void setBreakEvenMonths(double breakEvenMonths) {
        this.breakEvenMonths = breakEvenMonths;
    }

    public double getOfferPercentileAnchor() {
        return offerPercentileAnchor;
    }

    public void setOfferPercentileAnchor(double offerPercentileAnchor) {
        this.offerPercentileAnchor = offerPercentileAnchor;
    }

    public String getMarketAnchorLabel() {
        return marketAnchorLabel;
    }

    public void setMarketAnchorLabel(String marketAnchorLabel) {
        this.marketAnchorLabel = marketAnchorLabel;
    }

    public String getUnitTypeLabel() {
        return unitTypeLabel;
    }

    public void setUnitTypeLabel(String unitTypeLabel) {
        this.unitTypeLabel = unitTypeLabel;
    }

    public String getShiftGuaranteeLabel() {
        return shiftGuaranteeLabel;
    }

    public void setShiftGuaranteeLabel(String shiftGuaranteeLabel) {
        this.shiftGuaranteeLabel = shiftGuaranteeLabel;
    }

    public String getFloatRiskLabel() {
        return floatRiskLabel;
    }

    public void setFloatRiskLabel(String floatRiskLabel) {
        this.floatRiskLabel = floatRiskLabel;
    }

    public String getCancelRiskLabel() {
        return cancelRiskLabel;
    }

    public void setCancelRiskLabel(String cancelRiskLabel) {
        this.cancelRiskLabel = cancelRiskLabel;
    }

    public int getNurseScheduleRiskScore() {
        return nurseScheduleRiskScore;
    }

    public void setNurseScheduleRiskScore(int nurseScheduleRiskScore) {
        this.nurseScheduleRiskScore = nurseScheduleRiskScore;
    }

    public int getLifeFitRiskScore() {
        return lifeFitRiskScore;
    }

    public void setLifeFitRiskScore(int lifeFitRiskScore) {
        this.lifeFitRiskScore = lifeFitRiskScore;
    }

    public String getLifeFitLabel() {
        return lifeFitLabel;
    }

    public void setLifeFitLabel(String lifeFitLabel) {
        this.lifeFitLabel = lifeFitLabel;
    }

    public String getLifeFitSummary() {
        return lifeFitSummary;
    }

    public void setLifeFitSummary(String lifeFitSummary) {
        this.lifeFitSummary = lifeFitSummary;
    }

    public List<String> getLifeFitSignals() {
        return lifeFitSignals;
    }

    public void setLifeFitSignals(List<String> lifeFitSignals) {
        this.lifeFitSignals = lifeFitSignals;
    }

    public List<String> getRedFlags() {
        return redFlags;
    }

    public void setRedFlags(List<String> redFlags) {
        this.redFlags = redFlags;
    }

    public List<String> getHrQuestions() {
        return hrQuestions;
    }

    public void setHrQuestions(List<String> hrQuestions) {
        this.hrQuestions = hrQuestions;
    }

    public List<String> getNegotiationMoves() {
        return negotiationMoves;
    }

    public void setNegotiationMoves(List<String> negotiationMoves) {
        this.negotiationMoves = negotiationMoves;
    }

    public List<String> getVerdictReasons() {
        return verdictReasons;
    }

    public void setVerdictReasons(List<String> verdictReasons) {
        this.verdictReasons = verdictReasons;
    }

    public List<String> getDecisionLocks() {
        return decisionLocks;
    }

    public void setDecisionLocks(List<String> decisionLocks) {
        this.decisionLocks = decisionLocks;
    }

    public List<String> getSwingFactors() {
        return swingFactors;
    }

    public void setSwingFactors(List<String> swingFactors) {
        this.swingFactors = swingFactors;
    }

    public String getPacketSummary() {
        return packetSummary;
    }

    public void setPacketSummary(String packetSummary) {
        this.packetSummary = packetSummary;
    }

    public List<String> getTopRisks() {
        return topRisks;
    }

    public void setTopRisks(List<String> topRisks) {
        this.topRisks = topRisks;
    }

    public String getSurvivabilityHeadline() {
        return survivabilityHeadline;
    }

    public void setSurvivabilityHeadline(String survivabilityHeadline) {
        this.survivabilityHeadline = survivabilityHeadline;
    }

    public String getSurvivabilitySummary() {
        return survivabilitySummary;
    }

    public void setSurvivabilitySummary(String survivabilitySummary) {
        this.survivabilitySummary = survivabilitySummary;
    }

    public List<String> getSurvivabilitySignals() {
        return survivabilitySignals;
    }

    public void setSurvivabilitySignals(List<String> survivabilitySignals) {
        this.survivabilitySignals = survivabilitySignals;
    }

    public List<String> getMustAskNow() {
        return mustAskNow;
    }

    public void setMustAskNow(List<String> mustAskNow) {
        this.mustAskNow = mustAskNow;
    }

    public String getWalkAwayLine() {
        return walkAwayLine;
    }

    public void setWalkAwayLine(String walkAwayLine) {
        this.walkAwayLine = walkAwayLine;
    }
}
