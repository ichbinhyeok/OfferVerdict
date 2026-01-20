package com.offerverdict.service;

import com.offerverdict.OfferVerdictApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = OfferVerdictApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FinancialEngineTest {

    @Autowired
    private FinancialEngine financialEngine;

    @Test
    void calculatesLocalTax_NYC() {
        // NYC has 3.876% tax in StateTax.json
        double gross = 100_000.0;
        double expectedTax = 100_000.0 * 0.03876;

        double actualTax = financialEngine.calculateLocalTax(gross, "new-york-ny", null);

        assertEquals(expectedTax, actualTax, 0.01);
    }

    @Test
    void calculatesCarInsurance_Default() {
        // Default is 175.0 * 12 = 2100.0 in StateTax.json
        double actual = financialEngine.calculateCarInsurance("UNKNOWN_STATE", null);
        assertEquals(2100.0, actual, 0.01);
    }
}
