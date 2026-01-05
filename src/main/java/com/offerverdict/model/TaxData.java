package com.offerverdict.model;

import java.util.List;

public class TaxData {
    private FederalTax federal;
    private List<StateTax> states;
    private Fica fica;

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
