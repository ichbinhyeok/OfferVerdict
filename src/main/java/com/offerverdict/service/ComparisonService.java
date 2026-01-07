package com.offerverdict.service;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.JobInfo;
import com.offerverdict.model.LifestyleMetrics;
import com.offerverdict.model.Verdict;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
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
    private final TaxCalculatorService taxCalculatorService;
    private final CostCalculatorService costCalculatorService;
    private final RentDataService rentDataService;
    private final SalaryDataService salaryDataService;
    private final AppProperties appProperties;
    private final PurchasingPowerService purchasingPowerService; // Added missing field

    public ComparisonService(DataRepository repository,
            TaxCalculatorService taxCalculatorService,
            CostCalculatorService costCalculatorService,
            RentDataService rentDataService,
            SalaryDataService salaryDataService,
            AppProperties appProperties,
            PurchasingPowerService purchasingPowerService) { // Added missing param
        this.repository = repository;
        this.taxCalculatorService = taxCalculatorService;
        this.costCalculatorService = costCalculatorService;
        this.rentDataService = rentDataService;
        this.salaryDataService = salaryDataService;
        this.appProperties = appProperties;
        this.purchasingPowerService = purchasingPowerService;
    }

    public ComparisonResult compare(String cityASlug,
            String cityBSlug,
            double currentSalary,
            double offerSalary,
            HouseholdType householdType,
            HousingType housingType) {
        return compare(cityASlug, cityBSlug, currentSalary, offerSalary, householdType, housingType,
                null, null, null, 0.0, null);
    }

    public ComparisonResult compare(String citySlugA, String citySlugB, double salaryA, double salaryB,
            HouseholdType householdType, HousingType housingType, Boolean isMarried,
            Double fourOhOneKRate, Double monthlyInsurance, double studentLoanOrChildcare, Double rsuAmount) {

        CityCostEntry cityA = repository.getCity(citySlugA);
        CityCostEntry cityB = repository.getCity(citySlugB);

        // 1. Build Financial Breakdowns (The Evidence)
        ComparisonBreakdown breakdownA = buildBreakdown(salaryA, cityA, householdType, housingType, isMarried,
                fourOhOneKRate, monthlyInsurance, studentLoanOrChildcare, rsuAmount);
        ComparisonBreakdown breakdownB = buildBreakdown(salaryB, cityB, householdType, housingType, isMarried,
                fourOhOneKRate, monthlyInsurance, studentLoanOrChildcare, rsuAmount);

        ComparisonBreakdown current = breakdownA;
        ComparisonBreakdown offer = breakdownB;

        breakdownA.setGrossSalary(salaryA); // Ensure gross is set for leverage calc
        breakdownB.setGrossSalary(salaryB);

        // 2. Initialize Result & Verdict (The Hook)
        ComparisonResult result = new ComparisonResult();
        result.setCurrent(breakdownA);
        result.setOffer(breakdownB);

        // Calculate Verdict & Leverage using the Cash-Flow First logic
        purchasingPowerService.calculateVerdict(result);

        // Calculate detailed Residual comparison (The "Freedom Index")
        double residualA = breakdownA.getResidual();
        double residualB = breakdownB.getResidual();
        double currentHourlyRate = salaryA / 2080.0;
        double offerHourlyRate = salaryB / 2080.0;

        result.setCurrentLifestyle(new LifestyleMetrics(residualA, currentHourlyRate));
        result.setOfferLifestyle(new LifestyleMetrics(residualB, offerHourlyRate));

        // Calculate Delta Percent for Verdict Copy generation (if not set by service)
        // Service sets Verdict, Copy, MonthlyGain, ValueDiff.
        // We might need deltaPercent for internal logic or copy generation if service
        // didn't set it fully?
        // PurchasingPowerService sets specific copy, so we might skip
        // generateVerdictCopy or use it as fallback.
        // Let's rely on PurchasingPowerService for the core Verdict.

        // Note: updateVerdictWithAfterTaxReality is likely redundant now as
        // PurchasingPowerService includes tax/rent
        // But we can keep it as a safety check or remove it.
        // Removing it since PurchasingPowerService now uses the full breakdown.

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
                    String.format("Rent in %s is %s cheaper (Zillow)", cityBName, currency.format(rentDiff)));
        } else {
            result.setRentDiffMsg(
                    String.format("Rent in %s is %s more expensive", cityBName, currency.format(rentDiff)));
        }

        // Value (Real Hourly)
        // Adjust offer hourly by COL relative to Current? Or just use residual change?
        // Let's use Residual change per hour
        double residualHourlyChange = monthlyGain / 160.0; // 160 hours/mo
        if (residualHourlyChange < 0) {
            result.setValueDiffMsg(
                    String.format("Real hourly value drops by %s", currency.format(Math.abs(residualHourlyChange))));
        } else {
            result.setValueDiffMsg(
                    String.format("Real hourly value increases by %s", currency.format(residualHourlyChange)));
        }

        // Salary Benchmark
        // For now hardcode lookups for fallback
        // Salary Benchmark (Temporarily disabled due to build error)
        // result.setJobPercentile(salaryDataService.getPercentileLabel("software-engineer",
        // cityBSlug, offerSalary));
        // result.setGoodSalary(salaryDataService.isGoodSalary("software-engineer",
        // cityBSlug, offerSalary));

        return result;
    }

    private ComparisonBreakdown buildBreakdown(double salary,
            CityCostEntry city,
            HouseholdType householdType,
            HousingType housingType,
            Boolean isMarried,
            Double fourOhOneKRate,
            Double monthlyInsurance,
            double studentLoanOrChildcare,
            Double rsuAmount) {

        TaxCalculatorService.TaxResult taxResult = taxCalculatorService.calculateTax(
                salary,
                city.getState(),
                isMarried != null ? isMarried : (householdType == HouseholdType.FAMILY),
                fourOhOneKRate,
                monthlyInsurance,
                studentLoanOrChildcare > 0 ? studentLoanOrChildcare : null,
                rsuAmount);

        double netAnnual = taxResult.getNetIncome();
        double netMonthly = netAnnual / 12.0;
        double householdMultiplier = householdType == HouseholdType.FAMILY ? 1.4 : 1.0;

        double rent;
        double housingCost = 0.0;

        // USE RentDataService if available
        double zillowRent = rentDataService.getMedianRent(city.getSlug());

        switch (housingType) {
            case RENT:
                rent = zillowRent;
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
                rent = zillowRent;
        }

        double livingCost = costCalculatorService.calculateLivingCost(city, householdType);
        double groceries, transport, utilities, misc;

        if (city.getDetails() != null && !city.getDetails().isEmpty()) {
            groceries = city.getDetails().getOrDefault("groceries", 0.0) * householdMultiplier;
            transport = city.getDetails().getOrDefault("transport", 0.0) * householdMultiplier;
            utilities = city.getDetails().getOrDefault("utilities", 0.0) * householdMultiplier;
            misc = city.getDetails().getOrDefault("misc", 0.0) * householdMultiplier;
            livingCost = groceries + transport + utilities + misc;
        } else {
            groceries = livingCost * 0.30;
            transport = livingCost * 0.15;
            utilities = livingCost * 0.10;
            misc = livingCost - (groceries + transport + utilities);
        }

        double totalHousingCost = rent + housingCost;
        double residual = netMonthly - (totalHousingCost + livingCost);
        double monthlyResidual = residual;
        double yearsToBuyHouse = monthlyResidual > 0 ? (city.getAvgHousePrice() * 0.20) / (monthlyResidual * 12)
                : 999.0;
        double monthsToBuyTesla = monthlyResidual > 0 ? 50000.0 / monthlyResidual : 999.0;

        ComparisonBreakdown breakdown = new ComparisonBreakdown();
        breakdown.setNetMonthly(netMonthly);
        breakdown.setRent(totalHousingCost);
        breakdown.setLivingCost(livingCost);
        breakdown.setResidual(residual);
        breakdown.setGroceries(groceries);
        breakdown.setTransport(transport);
        breakdown.setUtilities(utilities);
        breakdown.setMisc(misc);
        breakdown.setYearsToBuyHouse(yearsToBuyHouse);
        breakdown.setMonthsToBuyTesla(monthsToBuyTesla);

        if (taxResult != null) {
            breakdown.setTaxResult(taxResult);
        }

        return breakdown;
    }

    public Verdict classifyVerdict(double deltaPercent) {
        if (deltaPercent >= 10) {
            return Verdict.GO;
        } else if (deltaPercent >= 0) {
            return Verdict.CONDITIONAL;
        } else if (deltaPercent > -10) {
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

    private String generateVerdictCopy(Verdict verdict, double deltaPercent) {
        double magnitude = Math.abs(deltaPercent);
        return switch (verdict) {
            case GO -> magnitude > 20
                    ? "You unlock " + Math.round(deltaPercent) + "% more life. Take the win."
                    : "You gain " + Math.round(deltaPercent) + "% more breathing room. Take it.";
            case CONDITIONAL -> "Slight edge at +" + Math.round(deltaPercent) + "%. Negotiate perks then go.";
            case WARNING -> "This move squeezes you by " + Math.abs(Math.round(deltaPercent)) + "%. Push back hard.";
            case NO_GO -> "This offer makes you " + Math.abs(Math.round(deltaPercent)) + "% poorer. Walk away.";
        };
    }

    public List<String> relatedCityComparisons(String jobSlug,
            String baseCitySlug,
            String offerCitySlug,
            String queryString) {
        JobInfo job = repository.getJob(jobSlug);
        String canonicalJob = job.getSlug();
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

    private void updateVerdictWithAfterTaxReality(ComparisonResult result, double residualA, double residualB) {
        double diff = residualB - residualA;
        // If Purchasing Power said GO, but After-Tax says NO...
        if (result.getVerdict() == com.offerverdict.model.Verdict.GO && diff < 0) {
            result.setVerdict(com.offerverdict.model.Verdict.WARNING);
            result.setVerdictCopy("TAX TRAP");
            result.setValueDiffMsg("Taxes kill the raise.");
        }
    }
}
