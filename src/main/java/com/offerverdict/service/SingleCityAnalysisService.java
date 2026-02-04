package com.offerverdict.service;

import com.offerverdict.config.AppProperties;
import com.offerverdict.model.AuthoritativeMetrics;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import org.springframework.stereotype.Service;

@Service
public class SingleCityAnalysisService {

    private final TaxCalculatorService taxCalculatorService;
    private final FinancialEngine financialEngine;
    private final AppProperties appProperties;

    public SingleCityAnalysisService(TaxCalculatorService taxCalculatorService,
            FinancialEngine financialEngine,
            AppProperties appProperties) {
        this.taxCalculatorService = taxCalculatorService;
        this.financialEngine = financialEngine;
        this.appProperties = appProperties;
    }

    public ComparisonBreakdown analyze(double salary,
            CityCostEntry city,
            AuthoritativeMetrics metrics,
            HouseholdType householdType,
            HousingType housingType,
            Boolean isMarried,
            Double fourOhOneKRate,
            Double monthlyInsurance,
            double studentLoanOrChildcare,
            double extraLeaks,
            double sideHustle,
            boolean isRemote,
            boolean isCarOwner,
            double signingBonus,
            double equityAnnual,
            double equityMultiplier,
            double commuteTime) {

        // Safety check for metrics
        if (metrics == null) {
            // Log warning or use fallback if possible (currently just creating empty
            // structure to avoid NPE)
            metrics = new AuthoritativeMetrics();
            // Ideally we should log this: Logger.warn("Metrics are null in
            // SingleCityAnalysisService");
        }

        TaxCalculatorService.TaxResult taxResult = taxCalculatorService.calculateTax(
                salary,
                city.getState(),
                isMarried != null ? isMarried : (householdType == HouseholdType.FAMILY),
                fourOhOneKRate,
                monthlyInsurance,
                studentLoanOrChildcare > 0 ? studentLoanOrChildcare * 12 : null,
                0.0);

        // CRITICAL FIX: Handle potential null result from tax calculator
        if (taxResult == null) {
            taxResult = new TaxCalculatorService.TaxResult();
            taxResult.setNetIncome(salary * 0.75); // Rough fallback (25% tax)
            // setTotalTax removed as it is undefined
            taxResult.setFederalTax(salary * 0.20);
            taxResult.setStateTax(salary * 0.05);
            taxResult.setFicaTax(0.0);
        }

        double netAnnual = taxResult.getNetIncome();

        // --- NEW AUTHORITATIVE ENRICHMENT ---
        double localTaxAnnual = 0.0;
        try {
            localTaxAnnual = financialEngine.calculateLocalTax(salary, city.getSlug(), metrics);
        } catch (Exception e) {
            // Ignore local tax error
            localTaxAnnual = 0.0;
        }

        double insuranceAnnual = 0.0;
        if (isCarOwner) {
            try {
                insuranceAnnual = financialEngine.calculateCarInsurance(city.getState(), metrics);
            } catch (Exception e) {
                insuranceAnnual = 1500.0; // Fallback
            }
        }

        // Amortized signing bonus & Equity scenario
        double annualEquity = equityAnnual * equityMultiplier;
        double amortizedSigning = signingBonus / 1.0; // Assume 1 year for first-year view

        double totalAnnualNet = netAnnual - localTaxAnnual - insuranceAnnual + annualEquity + amortizedSigning;
        double netMonthly = totalAnnualNet / 12.0;

        // Commute Time-Value Cost

        double householdMultiplier = householdType == HouseholdType.FAMILY ? 1.4 : 1.0;

        double rent;
        double housingCost = 0.0;

        // Use CityCost.json data directly
        double cityAvgRent = city.getAvgRent() > 0 ? city.getAvgRent() : 2000.0; // Fallback

        switch (housingType) {
            case RENT:
                rent = cityAvgRent;
                break;
            case OWN:
                rent = 0.0;
                housingCost = (city.getAvgHousePrice() * 0.015) / 12.0;
                break;
            case PARENTS:
                rent = 0.0;
                housingCost = 300.0;
                break;
            default:
                rent = cityAvgRent;
        }

        double livingCost = calculateLivingCost(city, householdType);
        double groceries, transport, utilities, misc;

        if (city.getDetails() != null && !city.getDetails().isEmpty()) {
            groceries = city.getDetails().getOrDefault("groceries", 0.0) * householdMultiplier;
            transport = city.getDetails().getOrDefault("transport", 0.0) * householdMultiplier;
            utilities = city.getDetails().getOrDefault("utilities", 0.0) * householdMultiplier;
            misc = city.getDetails().getOrDefault("misc", 0.0) * householdMultiplier;

            // Remote Work Discount (70% reduction in transport)
            if (isRemote) {
                transport *= 0.3;
            } else if (!isCarOwner) {
                // If not car owner (and not remote), assume public transit is cheaper (70% cost
                // of car ownership baseline)
                // This gives a "cash reward" for selling the car
                transport *= 0.3;
            }

            livingCost = groceries + transport + utilities + misc;
        } else {
            groceries = livingCost * 0.30;
            transport = livingCost * 0.15;
            if (isRemote) {
                transport *= 0.3;
            } else if (!isCarOwner) {
                transport *= 0.3;
            }
            utilities = livingCost * 0.10;
            misc = livingCost - (groceries + transport + utilities);
        }

        double totalHousingCost = rent + housingCost;

        // Residual = Net Income + Side Hustle - (Housing + Living + Debt)
        // [LOGIC CHANGE]: Subtracted 'monthlyCommuteCost' (Time Value) removed from
        // CASH calculation.
        // Commute Time is a "Quality of Life" penalty, not a cash penalty (unless we
        // track gas specifically, which is in Transport).
        double residual = (netMonthly + sideHustle)
                - (totalHousingCost + livingCost + studentLoanOrChildcare + extraLeaks);

        double monthlyResidual = residual;
        double yearsToBuyHouse = monthlyResidual > 0 ? (city.getAvgHousePrice() * 0.20) / (monthlyResidual * 12)
                : 99.0;
        double monthsToBuyTesla = monthlyResidual > 0 ? 50000.0 / monthlyResidual : 99.0;

        // Starbucks Index: $6/coffee * 22 working days = $132 potential savings
        double starbucksSavings = 6.0 * 22.0;

        ComparisonBreakdown breakdown = new ComparisonBreakdown();
        breakdown.setCityName(city.getCity());
        breakdown.setGrossSalary(salary);
        breakdown.setNetMonthly(netMonthly);
        breakdown.setRent(totalHousingCost);
        breakdown.setLivingCost(livingCost);
        breakdown.setResidual(residual);
        breakdown.setGroceries(groceries);
        breakdown.setTransport(transport);
        breakdown.setUtilities(utilities);
        breakdown.setMisc(misc);
        breakdown.setLocalTax(localTaxAnnual / 12.0);
        breakdown.setInsurance(insuranceAnnual / 12.0);
        breakdown.setYearsToBuyHouse(yearsToBuyHouse);
        breakdown.setMonthsToBuyTesla(monthsToBuyTesla);
        breakdown.setStarbucksSavings(starbucksSavings);

        breakdown.setMonthsToBuyTesla(monthsToBuyTesla);
        breakdown.setStarbucksSavings(starbucksSavings);

        breakdown.setEquityValue(annualEquity);
        breakdown.setSigningBonus(signingBonus);
        breakdown.setExtraLeaks(extraLeaks); // Set explicit leaks for visibility
        breakdown.setCommuteTime(commuteTime);

        // [LOGIC CHANGE]: Real Hourly Rate = Net Monthly / (Work Hours + Commute Hours)
        // Work Hours = ~173 (2080/12)
        // Commute Hours = (commuteTime * 2 * 22) / 60
        double workHoursMonthly = 2080.0 / 12.0;
        double commuteHoursMonthly = (commuteTime * 2 * 22) / 60.0;
        double trueHourlyRate = netMonthly / (workHoursMonthly + commuteHoursMonthly);

        breakdown.setRealHourlyRate(trueHourlyRate);

        if (taxResult != null) {
            breakdown.setTaxResult(taxResult);
        }

        return breakdown;
    }

    // Logic absorbed from CostCalculatorService
    private double calculateLivingCost(CityCostEntry city, HouseholdType householdType) {
        double baselineLivingCost = appProperties.getBaselineLivingCost();
        double multiplier = householdType == HouseholdType.FAMILY ? 1.4 : 1.0;
        return baselineLivingCost * (city.getColIndex() / 100.0) * multiplier;
    }
}
