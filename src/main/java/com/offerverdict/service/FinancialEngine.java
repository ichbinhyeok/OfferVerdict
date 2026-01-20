package com.offerverdict.service;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.AuthoritativeMetrics;
import com.offerverdict.model.ComparisonBreakdown;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FinancialEngine {

    private final DataRepository repository;

    public FinancialEngine(DataRepository repository) {
        this.repository = repository;
    }

    public double calculateLocalTax(double grossSalary, String citySlug, AuthoritativeMetrics metrics) {
        String lowerSlug = citySlug.toLowerCase();

        // 1. Central Configuration (Primary Source)
        Map<String, Double> localTaxes = repository.getTaxData().getLocalTaxes();
        if (localTaxes != null) {
            // New York City specific check
            if (lowerSlug.contains("new-york") && localTaxes.containsKey("nyc")) {
                return grossSalary * localTaxes.get("nyc");
            }
        }

        // 2. Legacy/Metrics Source (Fallback)
        if (metrics == null || metrics.getLocalIncomeTaxes() == null)
            return 0;

        // Explicit Special Cases (Common mismatches) from metrics
        if (lowerSlug.contains("new-york") && metrics.getLocalIncomeTaxes().containsKey("NYC")) {
            return grossSalary * metrics.getLocalIncomeTaxes().get("NYC");
        }

        // Generic Containment Match
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
        double defaultMonthly = 175.0;

        // Load default from Central Config
        if (repository.getTaxData().getDefaults() != null) {
            defaultMonthly = repository.getTaxData().getDefaults().getDefaultCarInsuranceMonthly();
        }

        if (metrics == null || metrics.getStateCarInsuranceMonthly() == null)
            return defaultMonthly * 12;

        Double monthly = metrics.getStateCarInsuranceMonthly().get(state.toUpperCase());
        if (monthly == null) {
            monthly = metrics.getStateCarInsuranceMonthly().getOrDefault("default", defaultMonthly);
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
