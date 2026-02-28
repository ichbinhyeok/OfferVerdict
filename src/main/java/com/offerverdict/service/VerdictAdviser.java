package com.offerverdict.service;

import com.offerverdict.model.Verdict;
import org.springframework.stereotype.Service;

@Service
public class VerdictAdviser {

    public String generateVerdictMessage(Verdict verdict, double deltaPercent, String cityB) {
        if (verdict == Verdict.GO) {
            return String.format(
                    "Under current assumptions, relocating to %s improves monthly purchasing-power residual by about %.1f%%.",
                    cityB, deltaPercent * 100);
        } else if (verdict == Verdict.NO_GO) {
            return String.format(
                    "Under current assumptions, moving to %s reduces purchasing-power residual. Reprice the offer before deciding.",
                    cityB);
        } else if (verdict == Verdict.WARNING) {
            return "Net gain is narrow after taxes and core costs. Treat this as a tight scenario and stress-test assumptions.";
        } else {
            return "This result is close to parity. Small changes in rent, commute, or taxes can flip the outcome.";
        }
    }

    public String getNegotiationLever(double gap) {
        if (gap <= 0)
            return "You are at or above modeled parity. Prioritize role scope, growth path, and downside protections.";
        return String.format(
                "Modeled parity requires about $%,.0f more in annual base compensation.",
                gap);
    }
}
