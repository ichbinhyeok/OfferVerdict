package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import com.offerverdict.service.SingleCityAnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SingleCityApiController {

    private final SingleCityAnalysisService analysisService;
    private final DataRepository dataRepository;

    public SingleCityApiController(SingleCityAnalysisService analysisService, DataRepository dataRepository) {
        this.analysisService = analysisService;
        this.dataRepository = dataRepository;
    }

    @PostMapping("/simulate-single")
    public SimulationResponse simulateSingle(@RequestBody SimulationRequest request) {
        CityCostEntry city = dataRepository.getCity(request.getCitySlug());
        if (city == null) {
            throw new IllegalArgumentException("Invalid city slug: " + request.getCitySlug());
        }

        ComparisonBreakdown result = analysisService.analyze(
                request.getSalary(),
                city,
                null, // AuthoritativeMetrics default
                HouseholdType.SINGLE, // Default for now
                HousingType.RENT, // Default
                request.getIsMarried(),
                request.getFourOhOneKRate(),
                request.getMonthlyInsurance(),
                0.0, // studentLoanOrChildcare (simplified)
                request.getOtherLeaks(),
                request.getSideHustle(),
                request.getIsRemote() != null ? request.getIsRemote() : false,
                true, // isCarOwner (default)
                request.getSigningBonus(),
                request.getEquityAnnual(),
                request.getEquityMultiplier(),
                request.getCommuteTime());

        // Dynamic Verdict Logic
        com.offerverdict.model.Verdict verdict;
        double residual = result.getResidual();
        double net = result.getNetMonthly();

        if (residual < 0)
            verdict = com.offerverdict.model.Verdict.NO_GO;
        else if (residual / net < 0.15)
            verdict = com.offerverdict.model.Verdict.WARNING;
        else if (residual / net < 0.30)
            verdict = com.offerverdict.model.Verdict.CONDITIONAL;
        else
            verdict = com.offerverdict.model.Verdict.GO;

        String verdictCssClass = "neutral-blue";
        if (verdict == com.offerverdict.model.Verdict.GO) {
            verdictCssClass = "premium-gold";
        } else if (verdict == com.offerverdict.model.Verdict.NO_GO) {
            verdictCssClass = "harsh-red";
        } else if (verdict == com.offerverdict.model.Verdict.WARNING) {
            verdictCssClass = "harsh-red"; // Re-using red for warning
        } else {
            verdictCssClass = "neutral-blue"; // Conditional
        }

        // Simple Verdict Text (e.g., "NO-GO", "WARNING")
        String verdictText = verdict.name().replace("_", "-");

        return new SimulationResponse(result, verdictText, verdictCssClass);
    }

    public static class SimulationResponse {
        public ComparisonBreakdown breakdown;
        public String verdictText;
        public String verdictCssClass;

        public SimulationResponse(ComparisonBreakdown breakdown, String verdictText, String verdictCssClass) {
            this.breakdown = breakdown;
            this.verdictText = verdictText;
            this.verdictCssClass = verdictCssClass;
        }
    }

    public static class SimulationRequest {
        private String citySlug;
        private double salary;
        private Boolean isMarried;
        private Double fourOhOneKRate;
        private Double monthlyInsurance;
        private Double signingBonus;
        private Double equityAnnual;
        private Double equityMultiplier;
        private Double commuteTime;
        private Double sideHustle;
        private Double otherLeaks;
        private Boolean isRemote;

        // Getters and Setters
        public String getCitySlug() {
            return citySlug;
        }

        public void setCitySlug(String citySlug) {
            this.citySlug = citySlug;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(double salary) {
            this.salary = salary;
        }

        public Boolean getIsMarried() {
            return isMarried;
        }

        public void setIsMarried(Boolean isMarried) {
            this.isMarried = isMarried;
        }

        public Double getFourOhOneKRate() {
            return fourOhOneKRate != null ? fourOhOneKRate : 0.0;
        }

        public void setFourOhOneKRate(Double fourOhOneKRate) {
            this.fourOhOneKRate = fourOhOneKRate;
        }

        public Double getMonthlyInsurance() {
            return monthlyInsurance != null ? monthlyInsurance : 0.0;
        }

        public void setMonthlyInsurance(Double monthlyInsurance) {
            this.monthlyInsurance = monthlyInsurance;
        }

        public Double getSigningBonus() {
            return signingBonus != null ? signingBonus : 0.0;
        }

        public void setSigningBonus(Double signingBonus) {
            this.signingBonus = signingBonus;
        }

        public Double getEquityAnnual() {
            return equityAnnual != null ? equityAnnual : 0.0;
        }

        public void setEquityAnnual(Double equityAnnual) {
            this.equityAnnual = equityAnnual;
        }

        public Double getEquityMultiplier() {
            return equityMultiplier != null ? equityMultiplier : 1.0;
        }

        public void setEquityMultiplier(Double equityMultiplier) {
            this.equityMultiplier = equityMultiplier;
        }

        public Double getCommuteTime() {
            return commuteTime != null ? commuteTime : 0.0;
        }

        public void setCommuteTime(Double commuteTime) {
            this.commuteTime = commuteTime;
        }

        public Double getSideHustle() {
            return sideHustle != null ? sideHustle : 0.0;
        }

        public void setSideHustle(Double sideHustle) {
            this.sideHustle = sideHustle;
        }

        public Double getOtherLeaks() {
            return otherLeaks != null ? otherLeaks : 0.0;
        }

        public void setOtherLeaks(Double otherLeaks) {
            this.otherLeaks = otherLeaks;
        }

        public Boolean getIsRemote() {
            return isRemote;
        }

        public void setIsRemote(Boolean isRemote) {
            this.isRemote = isRemote;
        }
    }
}
