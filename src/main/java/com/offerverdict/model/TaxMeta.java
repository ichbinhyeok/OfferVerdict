package com.offerverdict.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxMeta {
    private int taxYear;

    public int getTaxYear() {
        return taxYear;
    }

    public void setTaxYear(int taxYear) {
        this.taxYear = taxYear;
    }
}
