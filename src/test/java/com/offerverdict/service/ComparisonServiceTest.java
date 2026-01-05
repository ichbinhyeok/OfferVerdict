package com.offerverdict.service;

import com.offerverdict.OfferVerdictApplication;
import com.offerverdict.model.Verdict;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = OfferVerdictApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ComparisonServiceTest {

    @Autowired
    private ComparisonService comparisonService;

    @Test
    void classifiesVerdict() {
        assertEquals(Verdict.GO, comparisonService.classifyVerdict(12));
        assertEquals(Verdict.CONDITIONAL, comparisonService.classifyVerdict(5));
        assertEquals(Verdict.WARNING, comparisonService.classifyVerdict(-5));
        assertEquals(Verdict.NO_GO, comparisonService.classifyVerdict(-12));
    }
}
