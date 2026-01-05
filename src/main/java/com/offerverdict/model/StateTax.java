package com.offerverdict.model;

import java.util.List;

public class StateTax {
    private String state;
    private List<TaxBracket> brackets;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<TaxBracket> getBrackets() {
        return brackets;
    }

    public void setBrackets(List<TaxBracket> brackets) {
        this.brackets = brackets;
    }
}
