package com.offerverdict.service;

import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.Verdict;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class DynamicContentService {

    public String generateSingleCityIntro(ComparisonBreakdown breakdown) {
        String city = breakdown.getCityName();
        String salaryStr = String.format("$%,.0f", breakdown.getGrossSalary());
        double monthlyResidual = breakdown.getResidual();
        double annualTakeHome = breakdown.getNetMonthly() * 12;

        if (monthlyResidual < 0) {
            return String.format(
                    Locale.US,
                    "In %s, a %s salary is estimated to produce %s take-home pay per year but still runs a monthly deficit of %s after core costs.",
                    city,
                    salaryStr,
                    formatMoney(annualTakeHome),
                    formatMoney(Math.abs(monthlyResidual)));
        }

        return String.format(
                Locale.US,
                "In %s, a %s salary is estimated to produce %s take-home pay per year and leaves about %s monthly after core costs.",
                city,
                salaryStr,
                formatMoney(annualTakeHome),
                formatMoney(monthlyResidual));
    }

    public String generateHousingWarning(ComparisonBreakdown breakdown) {
        double housingRatio = safeRatio(breakdown.getRent(), breakdown.getNetMonthly());

        if (housingRatio > 0.45) {
            return "Housing costs are above 45% of take-home pay, which is a high-burden scenario.";
        }
        if (housingRatio > 0.35) {
            return "Housing costs are elevated and can limit savings flexibility.";
        }
        if (housingRatio < 0.20) {
            return "Housing costs are relatively efficient for this income level.";
        }
        return "Housing costs are within a typical range for a balanced budget.";
    }

    public String generateVerdictAnalysis(Verdict verdict, ComparisonBreakdown breakdown) {
        double savingsRate = safeRatio(breakdown.getResidual(), breakdown.getNetMonthly());

        if (verdict == Verdict.NO_GO) {
            return String.format(
                    Locale.US,
                    "At current assumptions, this scenario is not sustainable without reducing costs or increasing income in %s.",
                    breakdown.getCityName());
        }
        if (verdict == Verdict.WARNING) {
            return String.format(
                    Locale.US,
                    "This scenario is workable but tight. Estimated savings rate is %.1f%%, so unexpected costs could quickly reduce buffer.",
                    savingsRate * 100.0);
        }
        if (verdict == Verdict.CONDITIONAL) {
            return String.format(
                    Locale.US,
                    "This scenario is generally viable with an estimated savings rate of %.1f%% if spending assumptions hold.",
                    savingsRate * 100.0);
        }
        return String.format(
                Locale.US,
                "This scenario appears strong with an estimated savings rate of %.1f%% after taxes and core costs.",
                savingsRate * 100.0);
    }

    public String generateSingleCityVerdictMessage(Verdict verdict, ComparisonBreakdown breakdown) {
        double residual = breakdown.getResidual();
        if (verdict == Verdict.NO_GO) {
            return String.format(
                    Locale.US,
                    "At current assumptions, this budget runs short by about %s per month.",
                    formatMoney(Math.abs(residual)));
        }
        if (verdict == Verdict.WARNING) {
            return String.format(
                    Locale.US,
                    "This budget is positive but tight, leaving roughly %s per month after core costs.",
                    formatMoney(residual));
        }
        if (verdict == Verdict.CONDITIONAL) {
            return String.format(
                    Locale.US,
                    "This salary is workable, with roughly %s monthly residual after taxes and core costs.",
                    formatMoney(residual));
        }
        return String.format(
                Locale.US,
                "This scenario leaves roughly %s monthly residual after taxes and core costs.",
                formatMoney(residual));
    }

    private String formatMoney(double amount) {
        return String.format(Locale.US, "$%,.0f", amount);
    }

    private double safeRatio(double numerator, double denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return numerator / denominator;
    }
}
