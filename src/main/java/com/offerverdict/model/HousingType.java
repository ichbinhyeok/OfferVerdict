package com.offerverdict.model;

public enum HousingType {
    RENT,
    OWN;

    public double adjustedRent(double avgRent) {
        return switch (this) {
            case RENT -> avgRent;
            case OWN -> avgRent * 0.6;
        };
    }
}
