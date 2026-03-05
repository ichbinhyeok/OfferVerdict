package com.offerverdict.seo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeoUrlPolicyTest {

    @Test
    void alignToIntervalRoundsToNearestBucket() {
        assertEquals(120_000, SeoUrlPolicy.alignToInterval(123_456, 10_000));
        assertEquals(130_000, SeoUrlPolicy.alignToInterval(126_000, 10_000));
    }

    @Test
    void clampAndAlignKeepsSalaryInsideSeoRange() {
        assertEquals(30_000, SeoUrlPolicy.clampAndAlignSalary(12_000, 30_000, 500_000, 10_000));
        assertEquals(500_000, SeoUrlPolicy.clampAndAlignSalary(999_999, 30_000, 500_000, 10_000));
    }

    @Test
    void isWithinRangeHandlesBoundaries() {
        assertTrue(SeoUrlPolicy.isWithinRange(30_000, 30_000, 500_000));
        assertTrue(SeoUrlPolicy.isWithinRange(500_000, 30_000, 500_000));
        assertFalse(SeoUrlPolicy.isWithinRange(29_999, 30_000, 500_000));
    }
}
