package com.offerverdict.service;

import com.offerverdict.model.AuthoritativeMetrics;
import com.offerverdict.model.ComparisonBreakdown;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FinancialEngine {

    public double calculateLocalTax(double grossSalary, String citySlug, AuthoritativeMetrics metrics) {
        if (metrics == null || metrics.getLocalIncomeTaxes() == null)
            return 0;

        String lowerSlug = citySlug.toLowerCase();

        // 1. Explicit Special Cases (Common mismatches)
        if (lowerSlug.contains("new-york") && metrics.getLocalIncomeTaxes().containsKey("NYC")) {
            return grossSalary * metrics.getLocalIncomeTaxes().get("NYC");
        }

        // 2. Generic Containment Match
        for (Map.Entry<String, Double> entry : metrics.getLocalIncomeTaxes().entrySet()) {
            String key = entry.getKey().toLowerCase();
            // Match "Philadelphia" in "philadelphia-pa"
            if (lowerSlug.contains(key)) {
                return grossSalary * entry.getValue();
            }
        }
        return 0;
    }

    public double calculateCarInsurance(String state, AuthoritativeMetrics metrics) {
        if (metrics == null || metrics.getStateCarInsuranceMonthly() == null)
            return 175.0 * 12; // Default

        Double monthly = metrics.getStateCarInsuranceMonthly().get(state.toUpperCase());
        if (monthly == null) {
            monthly = metrics.getStateCarInsuranceMonthly().getOrDefault("default", 175.0);
        }
        return monthly * 12;
    }

    public double calculateSavingsPotential(ComparisonBreakdown breakdown, double extraBoosts, double extraLeaks) {
        // Savings = Gross - TotalTax - Rent - (Detailed Costs) + Boosts - Leaks
        double netAfterFixed = (breakdown.getNetMonthly() * 12) - (breakdown.getRent() * 12);
        double totalLivingCosts = breakdown.getLivingCost() * 12;

        return netAfterFixed - totalLivingCosts + extraBoosts - extraLeaks;
    }

    public double estimateBenefitValue(boolean premiumBenefits, double grossSalary, AuthoritativeMetrics metrics) {
        if (!premiumBenefits || metrics == null || metrics.getBenchmarks() == null)
            return 0;

        double matchValue = grossSalary * metrics.getBenchmarks().getAverage401kMatchPercent();
        double hsaValue = metrics.getBenchmarks().getAverageHSAContributionEmployer();

        return matchValue + hsaValue;
    }
}
