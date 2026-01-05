package com.offerverdict.service;

import com.offerverdict.config.AppProperties;
import com.offerverdict.model.CityCostEntry;
import org.springframework.stereotype.Service;

@Service
public class CostCalculatorService {
    private final double baselineLivingCost;

    public CostCalculatorService(AppProperties appProperties) {
        this.baselineLivingCost = appProperties.getBaselineLivingCost();
    }

    public double calculateLivingCost(CityCostEntry city) {
        return baselineLivingCost * (city.getColIndex() / 100.0);
    }
}
