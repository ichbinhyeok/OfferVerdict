package com.offerverdict.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlugNormalizerTest {

    @Test
    void normalizesCitySlug() {
        String normalized = SlugNormalizer.normalize("New York_NY ");
        assertEquals("new-york-ny", normalized);
        assertTrue(SlugNormalizer.isCanonicalCitySlug(normalized));
    }
}
