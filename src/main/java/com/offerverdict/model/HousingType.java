package com.offerverdict.model;

public enum HousingType {
    RENT,
    OWN,
    PARENTS;

    public double adjustedRent(double avgRent) {
        return switch (this) {
            case RENT -> avgRent;
            case OWN -> avgRent * 0.6;
            case PARENTS -> 0.0;
        };
    }
}
