package com.offerverdict.service;

import com.offerverdict.model.Verdict;
import org.springframework.stereotype.Service;

@Service
public class VerdictAdviser {

    public String generateVerdictMessage(Verdict verdict, double deltaPercent, String cityB) {
        if (verdict == Verdict.GO) {
            return String.format(
                    "A statistically superior move. Relocating to %s accelerates your wealth trajectory by %.1f%% and provides a clear strategic advantage.",
                    cityB, deltaPercent * 100);
        } else if (verdict == Verdict.NO_GO) {
            return String.format(
                    "High personal risk. Unless you negotiate at least $40,000 above parity, moving to %s is a regression in purchasing power.",
                    cityB);
        } else if (verdict == Verdict.WARNING) {
            return "Proceed with extreme caution. The nominal raise is almost entirely consumed by hidden cost-of-living leaks and localized inflation.";
        } else {
            return "This move is a lateral shift. Negotiate for a sign-on bonus or equity kicker to justify the transition risk.";
        }
    }

    public String getNegotiationLever(double gap) {
        if (gap <= 0)
            return "You have full leverage. Reiterate your value and focus on non-monetary perks.";
        return String.format(
                "Fiscal parity requires an additional $%,.0f in base compensation. Use this as your primary negotiation anchor.",
                gap);
    }
}
