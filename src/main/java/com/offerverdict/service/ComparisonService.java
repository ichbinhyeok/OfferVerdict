package com.offerverdict.service;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.AuthoritativeMetrics;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import com.offerverdict.model.JobInfo;
import com.offerverdict.model.LifestyleMetrics;
import com.offerverdict.model.Verdict;
import com.offerverdict.util.SlugNormalizer;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ComparisonService {
    private final DataRepository repository;
    private final TaxCalculatorService taxCalculatorService; // Kept for helper method
    private final AppProperties appProperties;
    private final FinancialEngine financialEngine;
    private final VerdictAdviser verdictAdviser;
    private final SingleCityAnalysisService singleCityAnalysisService;

    public ComparisonService(DataRepository repository,
            TaxCalculatorService taxCalculatorService,
            AppProperties appProperties,
            FinancialEngine financialEngine,
            VerdictAdviser verdictAdviser,
            SingleCityAnalysisService singleCityAnalysisService) {
        this.repository = repository;
        this.taxCalculatorService = taxCalculatorService;
        this.appProperties = appProperties;
        this.financialEngine = financialEngine;
        this.verdictAdviser = verdictAdviser;
        this.singleCityAnalysisService = singleCityAnalysisService;
    }

    public ComparisonResult compare(String cityASlug,
            String cityBSlug,
            double currentSalary,
            double offerSalary,
            HouseholdType householdType,
            HousingType housingType,
            Boolean isMarried,
            Double fourOhOneKRate,
            Double monthlyInsurance,
            double studentLoanOrChildcare,
            double sideHustle,
            boolean isRemote,
            boolean isCarOwner) {
        return compare(cityASlug, cityBSlug, currentSalary, offerSalary, householdType, housingType,
                isMarried, fourOhOneKRate, monthlyInsurance, studentLoanOrChildcare, 0.0, sideHustle, isRemote,
                isCarOwner,
                0.0, 0.0, 1.0, 0.0);
    }

    public ComparisonResult compare(String citySlugA, String citySlugB, double salaryA, double salaryB,
            HouseholdType householdType, HousingType housingType, Boolean isMarried,
            Double fourOhOneKRate, Double monthlyInsurance, double studentLoanOrChildcare,
            double offerSideLeaks,
            double sideHustle, boolean isRemote, boolean isCarOwner,
            double signingBonus, double equityAnnual, double equityMultiplier, double commuteTime) {

        CityCostEntry cityA = repository.getCity(citySlugA);
        CityCostEntry cityB = repository.getCity(citySlugB);
        AuthoritativeMetrics metrics = repository.getAuthoritativeMetrics();

        // 1. Build Financial Breakdowns (The Evidence)
        // [AUTHORITY UPGRADE]: Current is FIXED, Offer is SIMULATED

        // SingleCityAnalysisService Delegate
        ComparisonBreakdown breakdownA = singleCityAnalysisService.analyze(salaryA, cityA, metrics, householdType,
                housingType, isMarried,
                fourOhOneKRate, monthlyInsurance, studentLoanOrChildcare, 0.0, 0.0, false, true,
                0.0, 0.0, 1.0, 0.0);

        ComparisonBreakdown breakdownB = singleCityAnalysisService.analyze(salaryB, cityB, metrics, householdType,
                housingType, isMarried,
                fourOhOneKRate, monthlyInsurance, studentLoanOrChildcare, offerSideLeaks, sideHustle, isRemote,
                isCarOwner,
                signingBonus, equityAnnual, equityMultiplier, commuteTime);

        ComparisonBreakdown current = breakdownA;
        ComparisonBreakdown offer = breakdownB;

        // 2. Initialize Result & Verdict (The Hook)
        ComparisonResult result = new ComparisonResult();
        result.setCurrent(breakdownA);
        result.setOffer(breakdownB);

        // Calculate Verdict & Leverage using the Cash-Flow First logic
        calculateVerdict(result);

        // Calculate detailed Residual comparison (The "Freedom Index")
        double residualA = breakdownA.getResidual();
        double residualB = breakdownB.getResidual();
        double currentHourlyRate = salaryA / 2080.0;
        double offerHourlyRate = salaryB / 2080.0;

        result.setCurrentLifestyle(new LifestyleMetrics(residualA, currentHourlyRate));
        result.setOfferLifestyle(new LifestyleMetrics(residualB, offerHourlyRate));

        // Calculate Delta Percent just in case
        double deltaPercent = computeDeltaPercent(residualA, residualB);
        result.setDeltaPercent(deltaPercent);
        result.setMaxResidual(Math.max(residualA, residualB));

        // --- POPULATE RECEIPT FIELDS ---
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        currency.setMaximumFractionDigits(0);

        // 1. Monthly Gain
        double monthlyGain = offer.getResidual() - current.getResidual();
        String sign = monthlyGain >= 0 ? "+" : "";
        result.setMonthlyGainStr(sign + currency.format(monthlyGain));

        // 2. Freedom Index (Residual / Net Income)
        double freedomIdx = (offer.getResidual() / offer.getNetMonthly()) * 100;
        result.setFreedomIndex(String.format("%.0f%%", Math.max(0, freedomIdx)));

        // 3. Messages (Why?)
        String cityAName = cityA.getCity();
        String cityBName = cityB.getCity();

        // --- STRATEGIC METRICS: WEALTH BUFFER ---
        double monthlyDiff = offer.getResidual() - current.getResidual();
        // 10 Year Wealth projection at 7% return (0.07/12 = 0.00583 monthly)
        double r = 0.07 / 12.0;
        int n = 120; // 10 years
        double wealthGap = 0;
        if (monthlyDiff != 0) {
            wealthGap = monthlyDiff * ((Math.pow(1 + r, n) - 1) / r);
        }

        result.setInvestmentA(current.getResidual() * ((Math.pow(1 + r, n) - 1) / r));
        result.setInvestmentB(offer.getResidual() * ((Math.pow(1 + r, n) - 1) / r));

        if (monthlyDiff > 0) {
            result.setWealthBufferMsg(String.format("This move adds %s to your 10-year wealth at 7%% returns.",
                    currency.format(wealthGap)));
        } else if (monthlyDiff < 0) {
            result.setWealthBufferMsg(String.format("This move destroys %s of potential wealth in 10 years.",
                    currency.format(Math.abs(wealthGap))));
        } else {
            result.setWealthBufferMsg("No change in wealth trajectory.");
        }

        // --- CALC DELTAS (Simplified) ---
        offer.setSalaryDiff(salaryB - salaryA);
        offer.setTaxDiff((offer.getTaxResult().getTotalTax() + (offer.getLocalTax() * 12))
                - (current.getTaxResult().getTotalTax() + (current.getLocalTax() * 12)));
        offer.setHousingDiff((offer.getRent() * 12) - (current.getRent() * 12));
        offer.setResidualDiff((offer.getResidual() * 12) - (current.getResidual() * 12));

        // Tax Message
        double taxA = current.getTaxResult().getFederalTax() + current.getTaxResult().getStateTax();
        double taxB = offer.getTaxResult().getFederalTax() + offer.getTaxResult().getStateTax();
        double taxDiff = Math.abs(taxA - taxB) / 12.0;

        if (taxA > taxB) {
            result.setTaxDiffMsg(
                    String.format("%s takes %s more/mo than %s", cityAName, currency.format(taxDiff), cityBName));
        } else if (taxA < taxB) {
            result.setTaxDiffMsg(
                    String.format("%s takes %s more/mo than %s", cityBName, currency.format(taxDiff), cityAName));
        } else {
            result.setTaxDiffMsg("Tax impact is identical in both cities.");
        }

        // Rent Message
        double rentA = current.getRent();
        double rentB = offer.getRent();
        double rentDiff = Math.abs(rentA - rentB);

        if (rentA > rentB) {
            result.setRentDiffMsg(
                    String.format("Rent in %s is %s cheaper (CityCost)", cityBName, currency.format(rentDiff)));
        } else {
            result.setRentDiffMsg(
                    String.format("Rent in %s is %s more expensive", cityBName, currency.format(rentDiff)));
        }

        // Value (Real Hourly)
        double residualHourlyChange = monthlyGain / 160.0; // 160 hours/mo
        if (residualHourlyChange < 0) {
            result.setValueDiffMsg(
                    String.format("Real hourly value drops by %s", currency.format(Math.abs(residualHourlyChange))));
        } else {
            result.setValueDiffMsg(
                    String.format("Real hourly value increases by %s", currency.format(residualHourlyChange)));
        }

        return result;
    }

    // buildBreakdown and calculateLivingCost removed (moved to
    // SingleCityAnalysisService)

    // Logic absorbed from PurchasingPowerService
    private void calculateVerdict(ComparisonResult result) {
        AuthoritativeMetrics metrics = repository.getAuthoritativeMetrics();
        double currentResidual = result.getCurrent().getResidual();
        double offerResidual = result.getOffer().getResidual();

        // 1. Determine Verdict & Ad-hoc Color
        double diff = offerResidual - currentResidual;
        double deltaPercent = computeDeltaPercent(currentResidual, offerResidual);

        Verdict verdict = classifyVerdict(deltaPercent);
        result.setVerdict(verdict);
        result.setVerdictCopy(verdict.toString());

        // 2. Set Visual Tone
        String verdictColor = "neutral-blue"; // Default color
        if (verdict == Verdict.GO)
            result.setVerdictColor("premium-gold");
        else if (verdict == Verdict.NO_GO)
            result.setVerdictColor("harsh-red");
        else
            result.setVerdictColor(verdictColor);

        // 3. Authority Messaging
        result.setValueDiffMsg(
                verdictAdviser.generateVerdictMessage(verdict, deltaPercent / 100.0, result.getOffer().getCityName()));

        // 4. Reverse Calculation (The Magic Number)
        double offerEffectiveTaxRate = result.getOffer().getTaxResult().getEffectiveTaxRate();
        double gap = currentResidual - offerResidual;

        // Strict Authority Logic: Don't go if gain < $40,000 (roughly 3.3k/mo)
        double yearlyGain = diff * 12;
        if (verdict == Verdict.NO_GO || verdict == Verdict.WARNING || yearlyGain < 40000) {
            double reqGain = 40000.0; // The threshold
            double salaryNeeded = result.getOffer().getGrossSalary()
                    + ((reqGain - yearlyGain) / (1.0 - offerEffectiveTaxRate));
            result.setAuthorityAdvice(String.format("Don't go unless you negotiate at least $%,.0f more.",
                    salaryNeeded - result.getOffer().getGrossSalary()));
            result.setReverseSalaryGoal(Math.round(salaryNeeded / 1000.0) * 1000.0);
        } else {
            result.setAuthorityAdvice("Safe to proceed. This move meets the authoritative growth threshold.");
            result.setReverseSalaryGoal(result.getOffer().getGrossSalary());
        }

        result.setLeverageMsg(verdictAdviser.getNegotiationLever(gap * 12 / (1.0 - offerEffectiveTaxRate)));

        // 5. Benchmark Context (The Receipts)
        if (metrics != null) {
            result.setBenchmarkContext(String.format("Based on %s (Last updated: %s)",
                    metrics.getMetadata().source, metrics.getMetadata().lastUpdated));
        }

        result.setMonthlyGainStr((diff >= 0 ? "+" : "") + formatMoney(diff));
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount);
    }

    public Verdict classifyVerdict(double deltaPercent) {
        if (deltaPercent >= 5.0) {
            return Verdict.GO;
        } else if (deltaPercent >= -2.0) {
            return Verdict.CONDITIONAL;
        } else if (deltaPercent > -10.0) {
            return Verdict.WARNING;
        }
        return Verdict.NO_GO;
    }

    private double computeDeltaPercent(double residualA, double residualB) {
        if (residualA == 0) {
            return residualB == 0 ? 0 : residualB > 0 ? 100 : -100;
        }
        return (residualB - residualA) / Math.abs(residualA) * 100.0;
    }

    public List<String> relatedCityComparisons(String jobSlug,
            String baseCitySlug,
            String offerCitySlug,
            String queryString) {
        String canonicalJob = jobSlug;
        CityCostEntry origin = repository.getCity(baseCitySlug);
        return repository.getCities().stream()
                .filter(c -> !c.getSlug().equals(origin.getSlug()))
                .filter(c -> !c.getSlug().equals(offerCitySlug))
                .filter(c -> SlugNormalizer.isCanonicalCitySlug(c.getSlug()))
                .sorted(Comparator.comparing(CityCostEntry::getSlug))
                .limit(5)
                .map(c -> "/" + canonicalJob + "-salary-" + origin.getSlug() + "-vs-" + c.getSlug() + queryString)
                .toList();
    }

    public List<String> relatedJobComparisons(String cityASlug,
            String cityBSlug,
            String queryString) {
        return repository.getJobs().stream()
                .limit(5)
                .map(JobInfo::getSlug)
                .map(slug -> "/" + slug + "-salary-" + cityASlug + "-vs-" + cityBSlug + queryString)
                .collect(Collectors.toList());
    }

    public String buildCanonicalUrl(String path) {
        String base = appProperties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    public String formatCityName(CityCostEntry city) {
        return city.getCity() + ", " + city.getState().toUpperCase(Locale.US);
    }

    public TaxCalculatorService.TaxBreakdown getTaxBreakdown(double salary, String stateCode) {
        return taxCalculatorService.calculateTaxBreakdown(salary, stateCode);
    }
}
