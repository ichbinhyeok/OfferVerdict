package com.offerverdict.service;

import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.Verdict;
import org.springframework.stereotype.Service;

@Service
public class PurchasingPowerService {

    public ComparisonResult calculateVerdict(ComparisonResult result) {
        // 1. Extract Cash Flow (Residuals)
        double currentResidual = result.getCurrent().getResidual();
        double offerResidual = result.getOffer().getResidual();

        // 2. Calculate Break-Even Salary (Leverage)
        // How much Gross Salary in City B is needed to match CurrentResidual of City A?
        // TargetNet = CurrentResidual + OfferRent + OfferPreTax(Approx)
        // This is hard to reverse exactly without iteration since Tax is progressive.
        // Simplified approach: Use Offer City's Effective Tax Rate from the initial
        // calc.

        double offerEffectiveTaxRate = result.getOffer().getTaxResult().getEffectiveTaxRate(); // 0.25 (25%)
        double offerRentAnnual = result.getOffer().getRent() * 12;
        // Assume PreTax deductions are proportional or fixed? Let's assume fixed for
        // now ($1800 ins + 5% 401k)
        // Iterative approximation or simple scalar?
        // Let's use the Delta method:
        // Gap = OfferResidual - CurrentResidual.
        // We need to close this gap.
        // GrossNeed = Gap / (1 - MarginalTaxRate).
        // Marginal rate is safer than effective rate for the *next* dollar.
        // Let's assume a safe high marginal rate (e.g. 35% Fed + State) or use
        // Effective for simplicity.
        // LeverageSalary = OfferSalary + (CurrentResidual - OfferResidual) / (1 -
        // offerEffectiveTaxRate);

        double gap = currentResidual - offerResidual; // If positive, we are losing money.
        double targetGross = result.getOffer().getGrossSalary() + (gap * 12 / (1.0 - offerEffectiveTaxRate));

        // Round to nearest 100
        targetGross = Math.round(targetGross / 100.0) * 100.0;
        result.setBreakEvenSalary(targetGross);
        result.setLeverageMsg(String.format("Ask for $%s to match your current purchasing power.",
                String.format("%,d", (int) targetGross)));

        // 3. Determine Verdict based on Residual Diff
        double diff = offerResidual - currentResidual;

        if (diff > 0) {
            result.setVerdict(Verdict.GO);
            result.setVerdictCopy("GO");
            result.setValueDiffMsg("You get richer.");
            result.setMonthlyGainStr("+" + formatMoney(diff));
        } else {
            result.setVerdict(Verdict.NO_GO);
            result.setVerdictCopy("DON'T GO");
            result.setValueDiffMsg("You get poorer.");
            result.setMonthlyGainStr(formatMoney(diff));
        }

        return result;
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount);
    }
}
