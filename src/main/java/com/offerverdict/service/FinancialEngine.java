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

        // Match city slug or abbreviation
        for (Map.Entry<String, Double> entry : metrics.getLocalIncomeTaxes().entrySet()) {
            if (citySlug.toLowerCase().contains(entry.getKey().toLowerCase())) {
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
