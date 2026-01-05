package com.offerverdict.util;

import java.text.Normalizer;

public final class SlugNormalizer {
    private SlugNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String slug = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        slug = slug.trim()
                .replaceAll("[_\\s]+", "-")
                .replaceAll("[^a-zA-Z0-9-]", "")
                .replaceAll("-{2,}", "-")
                .toLowerCase();
        return slug.replaceAll("^-|-$", "");
    }

    public static boolean isCanonicalCitySlug(String slug) {
        return slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*-[a-z]{2}$");
    }
}
