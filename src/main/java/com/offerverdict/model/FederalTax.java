package com.offerverdict.model;

import java.util.List;

public class FederalTax {
    private List<TaxBracket> brackets;

    public List<TaxBracket> getBrackets() {
        return brackets;
    }

    public void setBrackets(List<TaxBracket> brackets) {
        this.brackets = brackets;
    }
}
