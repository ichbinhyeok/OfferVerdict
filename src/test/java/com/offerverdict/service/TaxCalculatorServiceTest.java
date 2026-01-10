package com.offerverdict.service;

import com.offerverdict.OfferVerdictApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = OfferVerdictApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TaxCalculatorServiceTest {

    @Autowired
    private TaxCalculatorService taxCalculatorService;

    @Test
    void calculatesNetAnnual() {
        double net = taxCalculatorService.calculateNetAnnual(120_000, "NY");
        assertEquals(80917.0, net, 1000);
    }
}
