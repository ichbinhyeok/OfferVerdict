package com.offerverdict.model;

public enum HouseholdType {
    SINGLE,
    FAMILY;

    public double multiplier() {
        return switch (this) {
            case SINGLE -> 1.0;
            case FAMILY -> 1.4;
        };
    }
}
