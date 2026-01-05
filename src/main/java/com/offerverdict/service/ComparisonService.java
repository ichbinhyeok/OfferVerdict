package com.offerverdict.service;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.JobInfo;
import com.offerverdict.model.Verdict;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import com.offerverdict.util.SlugNormalizer;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ComparisonService {
    private final DataRepository repository;
    private final TaxCalculatorService taxCalculatorService;
    private final CostCalculatorService costCalculatorService;
    private final AppProperties appProperties;

    public ComparisonService(DataRepository repository,
                             TaxCalculatorService taxCalculatorService,
                             CostCalculatorService costCalculatorService,
                             AppProperties appProperties) {
        this.repository = repository;
        this.taxCalculatorService = taxCalculatorService;
        this.costCalculatorService = costCalculatorService;
        this.appProperties = appProperties;
    }

    public ComparisonResult compare(String cityASlug,
                                    String cityBSlug,
                                    double currentSalary,
                                    double offerSalary,
                                    HouseholdType householdType,
                                    HousingType housingType) {
        CityCostEntry cityA = repository.getCity(cityASlug);
        CityCostEntry cityB = repository.getCity(cityBSlug);

        ComparisonBreakdown current = buildBreakdown(currentSalary, cityA, householdType, housingType);
        ComparisonBreakdown offer = buildBreakdown(offerSalary, cityB, householdType, housingType);

        double deltaPercent = computeDeltaPercent(current.getResidual(), offer.getResidual());
        Verdict verdict = classifyVerdict(deltaPercent);

        ComparisonResult result = new ComparisonResult();
        result.setCurrent(current);
        result.setOffer(offer);
        result.setDeltaPercent(deltaPercent);
        result.setVerdict(verdict);
        result.setVerdictCopy(generateVerdictCopy(verdict, deltaPercent));
        return result;
    }

    private ComparisonBreakdown buildBreakdown(double salary,
                                               CityCostEntry city,
                                               HouseholdType householdType,
                                               HousingType housingType) {
        double netAnnual = taxCalculatorService.calculateNetAnnual(salary, city.getState());
        double netMonthly = netAnnual / 12.0;
        double householdMultiplier = householdType == HouseholdType.FAMILY ? 1.4 : 1.0;
        double rentBaseline = housingType == HousingType.OWN ? city.getAvgRent() * 0.6 : city.getAvgRent();
        double rent = rentBaseline;
        double livingCost = costCalculatorService.calculateLivingCost(city, householdType);
        double groceries;
        double transport;
        double utilities;
        double misc;

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

        double residual = netMonthly - (rent + livingCost);
        double monthlyResidual = residual;
        double yearsToBuyHouse = monthlyResidual > 0 ? (city.getAvgHousePrice() * 0.20) / (monthlyResidual * 12) : 999.0;
        double monthsToBuyTesla = monthlyResidual > 0 ? 50000.0 / monthlyResidual : 999.0;

        ComparisonBreakdown breakdown = new ComparisonBreakdown();
        breakdown.setNetMonthly(netMonthly);
        breakdown.setRent(rent);
        breakdown.setLivingCost(livingCost);
        breakdown.setResidual(residual);
        breakdown.setGroceries(groceries);
        breakdown.setTransport(transport);
        breakdown.setUtilities(utilities);
        breakdown.setMisc(misc);
        breakdown.setYearsToBuyHouse(yearsToBuyHouse);
        breakdown.setMonthsToBuyTesla(monthsToBuyTesla);
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
}
