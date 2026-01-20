
package com.offerverdict.service;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.FederalTax;
import com.offerverdict.model.Fica;
import com.offerverdict.model.StateTax;
import com.offerverdict.model.TaxBracket;
import com.offerverdict.model.TaxData;
import com.offerverdict.model.TaxDefaults;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TaxCalculatorService - 2025 IRS Official Tax Data
 * Precision Engine for accurate tax calculations
 */
@Service
public class TaxCalculatorService {

    // Hardcoded constants removed in favor of TaxData defaults

    private final DataRepository repository;

    public TaxCalculatorService(DataRepository repository) {
        this.repository = repository;
    }

    /**
     * Calculate net annual income after all taxes
     * Uses 2025 IRS official data
     * 
     * @deprecated Use calculateTax() for detailed breakdown with pre-tax deductions
     */
    @Deprecated
    public double calculateNetAnnual(double salary, String stateCode) {
        // Step 1: Apply Standard Deduction (assuming Single for legacy)
        TaxData taxData = repository.getTaxData();
        double standardDeduction = taxData.getFederal().getStandardDeductionSingle();
        double taxableIncome = Math.max(0, salary - standardDeduction);

        // Step 2: Calculate Federal Tax using 2025 brackets
        double federalTax = calculateFederalTax(taxableIncome, false);

        // Step 3: Calculate State Tax
        double stateTax = calculateStateTax(salary, stateCode, false); // Legacy: uses gross income

        // Step 4: Calculate FICA (on gross, not taxable income)
        double ficaTax = calculateFICA(salary);

        // Step 5: Net income
        double totalTax = federalTax + stateTax + ficaTax;
        return Math.max(0, salary - totalTax);
    }

    /**
     * Calculate tax using US Tax Waterfall method
     * Waterfall: Gross -> Pre-tax Deductions -> FICA -> Taxable Income -> Net
     * 
     * @param grossIncome            Annual gross income (base salary)
     * @param stateCode              State code (e.g., "CA", "TX")
     * @param isMarried              true for Married filing jointly, false for
     *                               Single
     * @param preTax401kRate         Optional 401k contribution rate (0.0-1.0). If
     *                               null, uses default 5%
     * @param monthlyInsurance       Optional monthly health insurance cost. If
     *                               null, uses default $150/month
     * @param studentLoanOrChildcare Optional student loan or childcare deduction.
     *                               If null, uses 0
     * @param rsuAmount              Optional RSU amount. RSU is taxed separately
     *                               with 22% supplemental federal rate
     * @return TaxResult with detailed breakdown
     */
    public TaxResult calculateTax(double grossIncome, String stateCode, Boolean isMarried,
            Double preTax401kRate, Double monthlyInsurance, Double studentLoanOrChildcare,
            Double rsuAmount) {

        TaxData taxData = repository.getTaxData();
        TaxDefaults defaults = taxData.getDefaults();

        // Default values from Configuration (with safe fallbacks if config is missing)
        double default401kRate = 0.05;
        double max401kContribution = 23500.0; // 2025 fallback
        double defaultAnnualInsurance = 1800.0;
        double defaultRsuRate = 0.22;

        if (defaults != null) {
            max401kContribution = defaults.getMax401kContribution();
            defaultAnnualInsurance = defaults.getStandardMonthlyInsurance() * 12;
            defaultRsuRate = defaults.getRsuSupplementalRate();
        }

        // ============================================
        // Step 0: Apply Smart Defaults
        // ============================================
        boolean married = isMarried != null && isMarried;
        double effective401kRate = (preTax401kRate != null)
                ? preTax401kRate
                : default401kRate;
        double annualInsurance = (monthlyInsurance != null)
                ? monthlyInsurance * 12
                : defaultAnnualInsurance;
        double otherPreTaxDeductions = (studentLoanOrChildcare != null)
                ? studentLoanOrChildcare
                : 0.0;
        double rsuValue = (rsuAmount != null) ? rsuAmount : 0.0;

        // ============================================
        // Step A: Gross to Taxable Income
        // ============================================
        // Gross Income - Pre-tax Medical Insurance = FICA Taxable Base
        // NOTE: RSU is NOT subject to 401k deductions, but IS subject to FICA
        double ficaTaxableBase = grossIncome - annualInsurance + rsuValue; // RSU added to FICA base

        // Calculate 401k contribution (with IRS limit)
        // NOTE: 401k is calculated ONLY on base salary, NOT on RSU
        double preTax401k = Math.min(
                grossIncome * effective401kRate,
                max401kContribution);

        // FICA Taxable Base - 401k Contribution = Federal/State Taxable Income (base
        // salary only)
        // RSU is handled separately for federal tax (22% supplemental rate)
        double incomeAfterPreTaxDeductions = (grossIncome - annualInsurance) - preTax401k - otherPreTaxDeductions;
        double taxableIncome = Math.max(0, incomeAfterPreTaxDeductions);

        // ============================================
        // Step B: FICA Tax (includes RSU)
        // ============================================
        double ficaTax = calculateFICAWithMaritalStatus(ficaTaxableBase, married);

        // ============================================
        // Step C: Federal Income Tax
        // ============================================
        // Base salary: Progressive brackets
        double standardDeduction = married
                ? taxData.getFederal().getStandardDeductionMarried()
                : taxData.getFederal().getStandardDeductionSingle();
        double federalTaxableIncome = Math.max(0, taxableIncome - standardDeduction);
        double federalTaxOnSalary = calculateFederalTax(federalTaxableIncome, married);

        // RSU: Flat supplemental rate (NOT subject to progressive brackets)
        double rsuFederalTax = rsuValue * defaultRsuRate;
        double totalFederalTax = federalTaxOnSalary + rsuFederalTax;

        // ============================================
        // Step D: State Tax
        // ============================================
        // State tax is calculated on taxable income (base salary) + RSU
        // RSU is usually taxed at the same rate as ordinary income in most states
        double stateTaxableIncome = taxableIncome + rsuValue;
        double stateTax = calculateStateTax(stateTaxableIncome, stateCode, married);

        // ============================================
        // Step E: Calculate Net Income
        // ============================================
        double totalTax = ficaTax + totalFederalTax + stateTax;
        double totalGrossIncome = grossIncome + rsuValue;
        double netIncome = Math.max(0,
                totalGrossIncome - annualInsurance - preTax401k - otherPreTaxDeductions - totalTax);

        // ============================================
        // Step F: Build TaxResult
        // ============================================
        TaxResult result = new TaxResult();
        result.setGrossIncome(totalGrossIncome); // Include RSU in gross income
        result.setPreTax401k(preTax401k);
        result.setPreTaxInsurance(annualInsurance);
        result.setFicaTax(ficaTax);
        result.setFederalTax(totalFederalTax); // Includes both salary and RSU federal tax
        result.setRsuFederalTax(rsuFederalTax); // Separate RSU federal tax for breakdown
        result.setStateTax(stateTax);
        result.setNetIncome(netIncome);
        result.recalculateEffectiveTaxRate();

        return result;
    }

    /**
     * Get detailed tax breakdown for display
     * 
     * @deprecated Use calculateTax() for detailed breakdown with pre-tax deductions
     */
    @Deprecated
    public TaxBreakdown calculateTaxBreakdown(double salary, String stateCode) {
        TaxData taxData = repository.getTaxData();
        double standardDeduction = taxData.getFederal().getStandardDeductionSingle();
        double taxableIncome = Math.max(0, salary - standardDeduction);

        double federalTax = calculateFederalTax(taxableIncome, false); // Default to single for legacy
        double stateTax = calculateStateTax(salary, stateCode, false); // Legacy: uses gross income
        double ficaTax = calculateFICA(salary);

        // Recalculate components specifically for breakdown display if needed,
        // but leveraging helper methods ensures consistency.
        // FICA Breakdown for legacy
        Fica fica = taxData.getFica();
        double socialSecurity = Math.min(salary, fica.getSocialSecurityCap()) * fica.getSocialSecurityRate();
        double medicare = salary * fica.getMedicareRate();
        double additionalMedicare = salary > fica.getAdditionalMedicareThresholdSingle()
                ? (salary - fica.getAdditionalMedicareThresholdSingle()) * fica.getAdditionalMedicareRate()
                : 0;

        TaxBreakdown breakdown = new TaxBreakdown();
        breakdown.grossIncome = salary;
        breakdown.standardDeduction = standardDeduction;
        breakdown.taxableIncome = taxableIncome;
        breakdown.federalTax = federalTax;
        breakdown.stateTax = stateTax;
        breakdown.socialSecurityTax = socialSecurity;
        breakdown.medicareTax = medicare;
        breakdown.additionalMedicareTax = additionalMedicare;
        breakdown.totalTax = federalTax + stateTax + ficaTax;
        breakdown.netIncome = Math.max(0, salary - breakdown.totalTax);

        return breakdown;
    }

    /**
     * Calculate Federal Tax using 2025 IRS brackets
     */
    private double calculateFederalTax(double taxableIncome, boolean isMarried) {
        TaxData taxData = repository.getTaxData();
        FederalTax federal = taxData.getFederal();
        List<TaxBracket> brackets = isMarried ? federal.getBracketsMarried() : federal.getBracketsSingle();

        // Fallback for migration safety if married brackets are missing but single
        // exists
        if (brackets == null || brackets.isEmpty()) {
            brackets = federal.getBracketsSingle();
        }

        return computeTaxFromBrackets(taxableIncome, brackets);
    }

    /**
     * Calculate State Tax
     */
    private double calculateStateTax(double taxableIncome, String stateCode, boolean isMarried) {
        String state = stateCode.toUpperCase(Locale.US);
        Map<String, StateTax> stateTaxMap = repository.stateTaxMap();
        StateTax stateTax = stateTaxMap.get(state);

        if (stateTax != null) {
            List<TaxBracket> brackets = null;
            if (isMarried) {
                brackets = stateTax.getBracketsMarried();
            }
            // Fallback to single/default brackets if married not specified
            if (brackets == null || brackets.isEmpty()) {
                brackets = stateTax.getBrackets();
            }

            if (brackets != null && !brackets.isEmpty()) {
                return computeTaxFromBrackets(taxableIncome, brackets);
            }
        }

        // Default or Fallback if state not found or no brackets (should assume 0 or
        // handle logic)
        // With partial JSON, some states might be missing.
        // BUT we migrated ALL hardcoded states to JSON, so this should only hit for
        // unknown states.
        return 0.0;
    }

    /**
     * Calculate FICA (2025)
     */
    private double calculateFICAWithMaritalStatus(double ficaTaxableBase, boolean isMarried) {
        Fica fica = repository.getTaxData().getFica();

        // Social Security: 6.2% up to cap
        double socialSecurity = Math.min(ficaTaxableBase, fica.getSocialSecurityCap()) * fica.getSocialSecurityRate();

        // Medicare: 1.45% on all earnings (no cap)
        double medicare = ficaTaxableBase * fica.getMedicareRate();

        // Additional Medicare Tax (0.9% over threshold)
        double additionalMedicareThreshold = isMarried
                ? fica.getAdditionalMedicareThresholdMarried()
                : fica.getAdditionalMedicareThresholdSingle();
        double additionalMedicare = 0;
        if (ficaTaxableBase > additionalMedicareThreshold) {
            additionalMedicare = (ficaTaxableBase - additionalMedicareThreshold) * fica.getAdditionalMedicareRate();
        }

        return socialSecurity + medicare + additionalMedicare;
    }

    /**
     * Calculate FICA (2025) - Legacy method for backward compatibility
     */
    private double calculateFICA(double salary) {
        return calculateFICAWithMaritalStatus(salary, false); // Default to Single
    }

    /**
     * General method for bracket-based tax calculation
     */
    private double computeTaxFromBrackets(double taxableIncome, List<TaxBracket> brackets) {
        if (brackets == null || brackets.isEmpty())
            return 0.0;

        double tax = 0;
        double previousBracketLimit = 0;

        for (TaxBracket bracket : brackets) {
            Double cap = bracket.getUpTo();
            double rate = bracket.getRate();

            // Calculate taxable amount in this bracket
            // If cap is null, it means "infinity" (last bracket)
            double currentBracketLimit = (cap == null) ? Double.MAX_VALUE : cap;

            if (taxableIncome > previousBracketLimit) {
                double taxableInThisBracket = Math.min(taxableIncome, currentBracketLimit) - previousBracketLimit;
                tax += taxableInThisBracket * rate;
            }

            if (cap == null || taxableIncome <= cap) {
                break;
            }

            previousBracketLimit = currentBracketLimit;
        }
        return tax;
    }

    /**
     * Inner class for detailed tax breakdown
     */
    public static class TaxBreakdown {
        public double grossIncome;
        public double standardDeduction;
        public double taxableIncome;
        public double federalTax;
        public double stateTax;
        public double socialSecurityTax;
        public double medicareTax;
        public double additionalMedicareTax;
        public double totalTax;
        public double netIncome;

        public double getTotalFICA() {
            return socialSecurityTax + medicareTax + additionalMedicareTax;
        }

        public double getEffectiveTaxRate() {
            return grossIncome > 0 ? (totalTax / grossIncome) * 100 : 0;
        }
    }

    /**
     * TaxResult - Detailed US tax calculation result
     */
    public static class TaxResult {
        private double grossIncome;
        private double preTax401k;
        private double preTaxInsurance;
        private double ficaTax;
        private double federalTax;
        private double rsuFederalTax; // RSU-specific federal tax (22% supplemental rate)
        private double stateTax;
        private double netIncome;
        private double effectiveTaxRate;

        // Constructors
        public TaxResult() {
        }

        public TaxResult(double grossIncome, double preTax401k, double preTaxInsurance,
                double ficaTax, double federalTax, double stateTax, double netIncome) {
            this.grossIncome = grossIncome;
            this.preTax401k = preTax401k;
            this.preTaxInsurance = preTaxInsurance;
            this.ficaTax = ficaTax;
            this.federalTax = federalTax;
            this.stateTax = stateTax;
            this.netIncome = netIncome;
            this.effectiveTaxRate = grossIncome > 0 ? ((ficaTax + federalTax + stateTax) / grossIncome) * 100 : 0;
        }

        // Getters and Setters
        public double getGrossIncome() {
            return grossIncome;
        }

        public void setGrossIncome(double grossIncome) {
            this.grossIncome = grossIncome;
        }

        public double getPreTax401k() {
            return preTax401k;
        }

        public void setPreTax401k(double preTax401k) {
            this.preTax401k = preTax401k;
        }

        public double getPreTaxInsurance() {
            return preTaxInsurance;
        }

        public void setPreTaxInsurance(double preTaxInsurance) {
            this.preTaxInsurance = preTaxInsurance;
        }

        public double getFicaTax() {
            return ficaTax;
        }

        public void setFicaTax(double ficaTax) {
            this.ficaTax = ficaTax;
        }

        public double getFederalTax() {
            return federalTax;
        }

        public void setFederalTax(double federalTax) {
            this.federalTax = federalTax;
        }

        public double getStateTax() {
            return stateTax;
        }

        public void setStateTax(double stateTax) {
            this.stateTax = stateTax;
        }

        public double getRsuFederalTax() {
            return rsuFederalTax;
        }

        public void setRsuFederalTax(double rsuFederalTax) {
            this.rsuFederalTax = rsuFederalTax;
        }

        public double getNetIncome() {
            return netIncome;
        }

        public void setNetIncome(double netIncome) {
            this.netIncome = netIncome;
        }

        public double getTotalTax() {
            return ficaTax + federalTax + stateTax;
        }

        public double getEffectiveTaxRate() {
            return effectiveTaxRate;
        }

        public void setEffectiveTaxRate(double effectiveTaxRate) {
            this.effectiveTaxRate = effectiveTaxRate;
        }

        /**
         * Calculate and update effective tax rate based on current tax values
         */
        public void recalculateEffectiveTaxRate() {
            double totalTax = ficaTax + federalTax + stateTax;
            this.effectiveTaxRate = grossIncome > 0 ? (totalTax / grossIncome) * 100 : 0;
        }
    }
}
