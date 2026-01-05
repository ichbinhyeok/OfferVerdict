package com.offerverdict.util;

public final class MetaDescriptionUtil {
    private MetaDescriptionUtil() {
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1).trim() + "…";
    }
}
