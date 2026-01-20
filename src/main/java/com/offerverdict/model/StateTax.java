package com.offerverdict.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class StateTax {
    private String state;

    @JsonProperty("brackets") // Default to single
    private List<TaxBracket> brackets;

    @JsonProperty("brackets_married")
    private List<TaxBracket> bracketsMarried;

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

    public List<TaxBracket> getBracketsMarried() {
        return bracketsMarried;
    }

    public void setBracketsMarried(List<TaxBracket> bracketsMarried) {
        this.bracketsMarried = bracketsMarried;
    }
}
