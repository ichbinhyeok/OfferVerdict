package com.offerverdict.service;

import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.Verdict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DynamicContentService {

    private final Random random = new Random();

    public String generateSingleCityIntro(ComparisonBreakdown breakdown) {
        double residualRatio = breakdown.getResidual() / breakdown.getNetMonthly();
        String city = breakdown.getCityName();
        String salaryStr = String.format("$%,.0f", breakdown.getGrossSalary());

        List<String> options = new ArrayList<>();

        if (residualRatio < 0) {
            options.add(String.format("Making %s in %s poses a serious financial challenge based on current market rates.", salaryStr, city));
            options.add(String.format("Living in %s with a %s salary requires strict budgeting according to our latest cost models.", city, salaryStr));
        } else if (residualRatio < 0.2) {
            options.add(String.format("A salary of %s in %s offers a tight but manageable lifestyle.", salaryStr, city));
            options.add(String.format("Surviving in %s on %s is possible, but saving for the future will be slow.", city, salaryStr));
        } else if (residualRatio > 0.4) {
            options.add(String.format("Congratulations! %s is a powerful income in %s, unlocking rapid wealth generation.", salaryStr, city));
            options.add(String.format("%s in %s puts you in a commanding financial position.", salaryStr, city));
        } else {
            options.add(String.format("Earning %s in %s provides a standard middle-class lifestyle with some room for savings.", salaryStr, city));
            options.add(String.format("A %s income in %s is respectable, though inflation makes budgeting important.", salaryStr, city));
        }

        return options.get(random.nextInt(options.size()));
    }

    public String generateHousingWarning(ComparisonBreakdown breakdown) {
        double housingRatio = (breakdown.getRent()) / breakdown.getNetMonthly();

        if (housingRatio > 0.45) {
            return "WARNING: Housing costs consume over 45% of your net income. This is considered 'Rent Burdened' and high risk.";
        } else if (housingRatio > 0.35) {
            return "Note: Housing expenses are on the higher side (>35%), leaving less room for discretionary spending.";
        } else if (housingRatio < 0.20) {
            return "Excellent: Housing is very affordable here, taking less than 20% of your take-home pay.";
        }
        return "Housing costs are within the standard recommended range (approx. 30%).";
    }

    public String generateVerdictAnalysis(Verdict verdict, ComparisonBreakdown breakdown) {
        if (verdict == Verdict.NO_GO) {
            return String.format("Our algorithms strongly advise against this move to %s without a significantly higher offer.", breakdown.getCityName());
        } else if (verdict == Verdict.GO) {
            return String.format("This is a green-light opportunity. %s offers a clear financial advantage.", breakdown.getCityName());
        }
        return "This scenario has pros and cons. Review the detailed breakdown below to see if the lifestyle trade-offs are worth it.";
    }
}
