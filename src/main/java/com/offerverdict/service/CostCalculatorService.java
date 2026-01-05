package com.offerverdict.service;

import com.offerverdict.config.AppProperties;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.HouseholdType;
import org.springframework.stereotype.Service;

@Service
public class CostCalculatorService {
    private final double baselineLivingCost;

    public CostCalculatorService(AppProperties appProperties) {
        this.baselineLivingCost = appProperties.getBaselineLivingCost();
    }

    public double calculateLivingCost(CityCostEntry city) {
        return calculateLivingCost(city, HouseholdType.SINGLE);
    }

    public double calculateLivingCost(CityCostEntry city, HouseholdType householdType) {
        double multiplier = householdType == HouseholdType.FAMILY ? 1.4 : 1.0;
        return baselineLivingCost * (city.getColIndex() / 100.0) * multiplier;
    }
}
