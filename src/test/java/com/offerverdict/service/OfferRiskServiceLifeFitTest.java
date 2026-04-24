package com.offerverdict.service;

import com.offerverdict.OfferVerdictApplication;
import com.offerverdict.model.OfferRiskReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OfferVerdictApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OfferRiskServiceLifeFitTest {

    @Autowired
    private OfferRiskService offerRiskService;

    @Test
    void familySeparationPreventsAcceptableVerdictEvenWhenPayImproves() {
        OfferRiskReport report = offerRiskService.assess(
                "offer_review",
                "registered-nurse",
                "austin-tx",
                "seattle-wa",
                "icu",
                "written",
                "home_unit_only",
                "protected_hours",
                42,
                85,
                36,
                0,
                12,
                36,
                0,
                0,
                150,
                150,
                0,
                0,
                5000,
                0,
                0,
                "none",
                "The pay is higher, but my family would stay in Austin for the first year and I am worried night shift will make this hard on my kids.");

        assertEquals("NEGOTIATE", report.getVerdict());
        assertTrue(report.getLifeFitRiskScore() >= 7);
        assertTrue(report.getDecisionLocks().stream().anyMatch(lock -> lock.contains("family")));
        assertTrue(report.getTopRisks().stream().anyMatch(risk -> risk.contains("Life fit")));
    }

    @Test
    void severeLifeFitPlusClawbackCanTriggerWalkAway() {
        OfferRiskReport report = offerRiskService.assess(
                "offer_review",
                "registered-nurse",
                "austin-tx",
                "seattle-wa",
                "icu",
                "rotating",
                "hospital_wide",
                "unknown",
                42,
                60,
                36,
                0,
                12,
                36,
                0,
                0,
                150,
                150,
                15000,
                4000,
                7000,
                24,
                12,
                "prorated",
                "My spouse and kids would stay behind, childcare is already tight, and I heard the unit is short staffed with no breaks and a toxic culture.");

        assertEquals("WALK AWAY", report.getVerdict());
        assertTrue(report.getLifeFitRiskScore() >= 7);
        assertFalse(report.getLifeFitSignals().isEmpty());
        assertTrue(report.getWalkAwayLine().contains("family"));
    }
}
