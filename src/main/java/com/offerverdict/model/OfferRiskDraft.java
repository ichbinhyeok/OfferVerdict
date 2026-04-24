package com.offerverdict.model;

public class OfferRiskDraft {
    private String analysisMode;
    private String sourceText;
    private String roleSlug;
    private String currentCitySlug;
    private String offerCitySlug;
    private String unitType;
    private String shiftGuarantee;
    private String floatRisk;
    private String cancelRisk;
    private double currentHourlyRate;
    private double offerHourlyRate;
    private double weeklyHours;
    private double overtimeHours;
    private double nightDiffPercent;
    private double nightHours;
    private double weekendDiffPercent;
    private double weekendHours;
    private double currentMonthlyInsurance;
    private double offerMonthlyInsurance;
    private double signOnBonus;
    private double relocationStipend;
    private double movingCost;
    private int contractMonths;
    private int plannedStayMonths;
    private String repaymentStyle;

    public static OfferRiskDraft manualDefaults(String analysisMode) {
        OfferRiskDraft draft = new OfferRiskDraft();
        draft.setAnalysisMode(normalizeMode(analysisMode));
        draft.setRoleSlug("registered-nurse");
        draft.setCurrentCitySlug("austin-tx");
        draft.setOfferCitySlug("seattle-wa");
        draft.setUnitType("med_surg");
        draft.setShiftGuarantee("written");
        draft.setFloatRisk("home_unit_only");
        draft.setCancelRisk("protected_hours");
        draft.setCurrentHourlyRate(42);
        draft.setOfferHourlyRate(56);
        draft.setWeeklyHours(36);
        draft.setOvertimeHours(4);
        draft.setNightDiffPercent(12);
        draft.setNightHours(18);
        draft.setWeekendDiffPercent(8);
        draft.setWeekendHours(8);
        draft.setCurrentMonthlyInsurance(150);
        draft.setOfferMonthlyInsurance(150);
        draft.setSignOnBonus(15000);
        draft.setRelocationStipend(5000);
        draft.setMovingCost(7000);
        draft.setContractMonths(24);
        draft.setPlannedStayMonths(12);
        draft.setRepaymentStyle("prorated");
        if ("job_post".equals(draft.getAnalysisMode())) {
            draft.setOfferHourlyRate(54);
            draft.setOfferMonthlyInsurance(0);
            draft.setCurrentMonthlyInsurance(0);
            draft.setOvertimeHours(0);
            draft.setNightDiffPercent(0);
            draft.setNightHours(0);
            draft.setWeekendDiffPercent(0);
            draft.setWeekendHours(0);
            draft.setSignOnBonus(0);
            draft.setRelocationStipend(0);
            draft.setMovingCost(0);
            draft.setContractMonths(0);
            draft.setPlannedStayMonths(0);
            draft.setShiftGuarantee("unknown");
            draft.setFloatRisk("unknown");
            draft.setCancelRisk("unknown");
        }
        return draft;
    }

    public static OfferRiskDraft parsedDefaults(String sourceText, String analysisMode) {
        OfferRiskDraft draft = manualDefaults(analysisMode);
        draft.setSourceText(sourceText);
        draft.setOvertimeHours(0);
        draft.setNightDiffPercent(0);
        draft.setNightHours(0);
        draft.setWeekendDiffPercent(0);
        draft.setWeekendHours(0);
        draft.setSignOnBonus(0);
        draft.setRelocationStipend(0);
        draft.setMovingCost(0);
        draft.setContractMonths(0);
        return draft;
    }

    private static String normalizeMode(String analysisMode) {
        return "job_post".equalsIgnoreCase(analysisMode) ? "job_post" : "offer_review";
    }

    public String getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(String analysisMode) {
        this.analysisMode = normalizeMode(analysisMode);
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getRoleSlug() {
        return roleSlug;
    }

    public void setRoleSlug(String roleSlug) {
        this.roleSlug = roleSlug;
    }

    public String getCurrentCitySlug() {
        return currentCitySlug;
    }

    public void setCurrentCitySlug(String currentCitySlug) {
        this.currentCitySlug = currentCitySlug;
    }

    public String getOfferCitySlug() {
        return offerCitySlug;
    }

    public void setOfferCitySlug(String offerCitySlug) {
        this.offerCitySlug = offerCitySlug;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getShiftGuarantee() {
        return shiftGuarantee;
    }

    public void setShiftGuarantee(String shiftGuarantee) {
        this.shiftGuarantee = shiftGuarantee;
    }

    public String getFloatRisk() {
        return floatRisk;
    }

    public void setFloatRisk(String floatRisk) {
        this.floatRisk = floatRisk;
    }

    public String getCancelRisk() {
        return cancelRisk;
    }

    public void setCancelRisk(String cancelRisk) {
        this.cancelRisk = cancelRisk;
    }

    public double getCurrentHourlyRate() {
        return currentHourlyRate;
    }

    public void setCurrentHourlyRate(double currentHourlyRate) {
        this.currentHourlyRate = currentHourlyRate;
    }

    public double getOfferHourlyRate() {
        return offerHourlyRate;
    }

    public void setOfferHourlyRate(double offerHourlyRate) {
        this.offerHourlyRate = offerHourlyRate;
    }

    public double getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(double weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public double getNightDiffPercent() {
        return nightDiffPercent;
    }

    public void setNightDiffPercent(double nightDiffPercent) {
        this.nightDiffPercent = nightDiffPercent;
    }

    public double getNightHours() {
        return nightHours;
    }

    public void setNightHours(double nightHours) {
        this.nightHours = nightHours;
    }

    public double getWeekendDiffPercent() {
        return weekendDiffPercent;
    }

    public void setWeekendDiffPercent(double weekendDiffPercent) {
        this.weekendDiffPercent = weekendDiffPercent;
    }

    public double getWeekendHours() {
        return weekendHours;
    }

    public void setWeekendHours(double weekendHours) {
        this.weekendHours = weekendHours;
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
}
