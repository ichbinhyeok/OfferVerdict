package com.offerverdict.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxBracket {
    private Double upTo;
    private double rate;

    public Double getUpTo() {
        return upTo;
    }

    public void setUpTo(Double upTo) {
        this.upTo = upTo;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
}
