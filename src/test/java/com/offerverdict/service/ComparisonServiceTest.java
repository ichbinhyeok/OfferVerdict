package com.offerverdict.service;

import com.offerverdict.OfferVerdictApplication;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import com.offerverdict.model.Verdict;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = OfferVerdictApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ComparisonServiceTest {

    @Autowired
    private ComparisonService comparisonService;

    @Test
    void classifiesVerdict() {
        assertEquals(Verdict.GO, comparisonService.classifyVerdict(12));
        assertEquals(Verdict.CONDITIONAL, comparisonService.classifyVerdict(4));
        assertEquals(Verdict.WARNING, comparisonService.classifyVerdict(-5));
        assertEquals(Verdict.NO_GO, comparisonService.classifyVerdict(-12));
    }

    @Test
    void monthlyGainStringShowsCurrencySignAndUnit() {
        var result = comparisonService.compare(
                "austin-tx",
                "san-francisco-ca",
                120000,
                150000,
                HouseholdType.SINGLE,
                HousingType.RENT,
                false,
                0.0,
                0.0,
                0.0,
                0.0,
                false,
                true);

        assertTrue(result.getMonthlyGainStr().matches("^[+-]\\$[0-9,]+/mo$"));
    }
}
