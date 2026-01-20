package com.offerverdict.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FederalTax {
    @JsonProperty("brackets_single")
    private List<TaxBracket> bracketsSingle;

    @JsonProperty("brackets_married")
    private List<TaxBracket> bracketsMarried;

    @JsonProperty("standard_deduction_single")
    private double standardDeductionSingle;

    @JsonProperty("standard_deduction_married")
    private double standardDeductionMarried;

    // Legacy support for older JSON format temporarily if needed, but we are doing
    // full migration
    @JsonProperty("brackets") // Keep this only if you want to verify migration, otherwise remove
    private List<TaxBracket> legacyBrackets;

    public List<TaxBracket> getBracketsSingle() {
        return bracketsSingle;
    }

    public void setBracketsSingle(List<TaxBracket> bracketsSingle) {
        this.bracketsSingle = bracketsSingle;
    }

    public List<TaxBracket> getBracketsMarried() {
        return bracketsMarried;
    }

    public void setBracketsMarried(List<TaxBracket> bracketsMarried) {
        this.bracketsMarried = bracketsMarried;
    }

    public double getStandardDeductionSingle() {
        return standardDeductionSingle;
    }

    public void setStandardDeductionSingle(double standardDeductionSingle) {
        this.standardDeductionSingle = standardDeductionSingle;
    }

    public double getStandardDeductionMarried() {
        return standardDeductionMarried;
    }

    public void setStandardDeductionMarried(double standardDeductionMarried) {
        this.standardDeductionMarried = standardDeductionMarried;
    }
}
