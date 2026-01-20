package com.offerverdict.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxData {
    private TaxMeta meta;
    private TaxDefaults defaults;

    @JsonProperty("local_taxes")
    private java.util.Map<String, Double> localTaxes;

    private FederalTax federal;
    private List<StateTax> states;
    private Fica fica;

    public TaxMeta getMeta() {
        return meta;
    }

    public void setMeta(TaxMeta meta) {
        this.meta = meta;
    }

    public TaxDefaults getDefaults() {
        return defaults;
    }

    public void setDefaults(TaxDefaults defaults) {
        this.defaults = defaults;
    }

    public java.util.Map<String, Double> getLocalTaxes() {
        return localTaxes;
    }

    public void setLocalTaxes(java.util.Map<String, Double> localTaxes) {
        this.localTaxes = localTaxes;
    }

    public FederalTax getFederal() {
        return federal;
    }

    public void setFederal(FederalTax federal) {
        this.federal = federal;
    }

    public List<StateTax> getStates() {
        return states;
    }

    public void setStates(List<StateTax> states) {
        this.states = states;
    }

    public Fica getFica() {
        return fica;
    }

    public void setFica(Fica fica) {
        this.fica = fica;
    }
}
