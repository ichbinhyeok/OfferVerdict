package com.offerverdict.service;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.StateTax;
import com.offerverdict.model.TaxBracket;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TaxCalculatorService - 2025 IRS Official Tax Data
 * Precision Engine for accurate tax calculations
 */
@Service
public class TaxCalculatorService {
    // ============================================
    // 2025 IRS OFFICIAL TAX DATA
    // ============================================

    // Standard Deductions (2025)
    private static final double STANDARD_DEDUCTION_SINGLE_2025 = 14_600.0;
    private static final double STANDARD_DEDUCTION_MARRIED_2025 = 29_200.0;

    // 401k Contribution Limits (2024-2025)
    private static final double MAX_401K_CONTRIBUTION_2024 = 23_000.0;

    // RSU Supplemental Tax Rate (Federal)
    private static final double RSU_SUPPLEMENTAL_FEDERAL_RATE = 0.22; // 22% flat rate

    // Smart Defaults (Industry Standard)
    private static final double DEFAULT_401K_RATE = 0.05; // 5%
    private static final double DEFAULT_MONTHLY_INSURANCE = 150.0; // $150/month
    private static final double DEFAULT_ANNUAL_INSURANCE = DEFAULT_MONTHLY_INSURANCE * 12; // $1,800/year

    // Federal Tax Brackets (2025 Single Filer)
    private static final double[][] FEDERAL_BRACKETS_2025_SINGLE = {
            { 11_925, 0.10 }, // 10%: $0 – $11,925
            { 48_475, 0.12 }, // 12%: $11,926 – $48,475
            { 103_350, 0.22 }, // 22%: $48,476 – $103,350
            { 197_300, 0.24 }, // 24%: $103,351 – $197,300
            { 250_525, 0.32 }, // 32%: $197,301 – $250,525
            { 626_350, 0.35 }, // 35%: $250,526 – $626,350
            { Double.MAX_VALUE, 0.37 } // 37%: Over $626,350
    };

    // Federal Tax Brackets (2025 Married Filing Jointly)
    private static final double[][] FEDERAL_BRACKETS_2025_MARRIED = {
            { 23_850, 0.10 }, // 10%: $0 – $23,850
            { 96_950, 0.12 }, // 12%: $23,851 – $96,950
            { 206_700, 0.22 }, // 22%: $96,951 – $206,700
            { 394_600, 0.24 }, // 24%: $206,701 – $394,600
            { 501_050, 0.32 }, // 32%: $394,601 – $501,050
            { 751_600, 0.35 }, // 35%: $501,051 – $751,600
            { Double.MAX_VALUE, 0.37 } // 37%: Over $751,600
    };

    // FICA (2025)
    private static final double SOCIAL_SECURITY_RATE = 0.062; // 6.2%
    private static final double SOCIAL_SECURITY_CAP = 176_100.0; // 2025 limit
    private static final double MEDICARE_RATE = 0.0145; // 1.45%
    private static final double ADDITIONAL_MEDICARE_THRESHOLD_SINGLE = 200_000.0;
    private static final double ADDITIONAL_MEDICARE_THRESHOLD_MARRIED = 250_000.0;
    private static final double ADDITIONAL_MEDICARE_RATE = 0.009; // 0.9%

    // State Tax Strategy
    private static final String[] NO_TAX_STATES = { "TX", "FL", "NV", "TN", "WA", "WY", "SD", "AK", "NH" };
    private static final Map<String, Double> FLAT_TAX_STATES = Map.of(
            "PA", 0.0307, // Pennsylvania 3.07%
            "IL", 0.0495, // Illinois 4.95%
            "MA", 0.05, // Massachusetts 5%
            "MI", 0.0425, // Michigan 4.25%
            "IN", 0.0305, // Indiana 3.05%
            "CO", 0.044, // Colorado 4.4%
            "NC", 0.0475, // North Carolina 4.75%
            "UT", 0.0465 // Utah 4.65%
    );

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
    public double calculateNetAnnual(double salary, String stateCode) {
        // Step 1: Apply Standard Deduction
        double taxableIncome = Math.max(0, salary - STANDARD_DEDUCTION_SINGLE_2025);

        // Step 2: Calculate Federal Tax using 2025 brackets
        double federalTax = calculateFederalTax(taxableIncome, false);

        // Step 3: Calculate State Tax
        double stateTax = calculateStateTax(salary, stateCode); // Legacy: uses gross income

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
        // ============================================
        // Step 0: Apply Smart Defaults
        // ============================================
        boolean married = isMarried != null && isMarried;
        double effective401kRate = (preTax401kRate != null && preTax401kRate > 0)
                ? preTax401kRate
                : DEFAULT_401K_RATE;
        double annualInsurance = (monthlyInsurance != null && monthlyInsurance > 0)
                ? monthlyInsurance * 12
                : DEFAULT_ANNUAL_INSURANCE;
        double otherPreTaxDeductions = (studentLoanOrChildcare != null && studentLoanOrChildcare > 0)
                ? studentLoanOrChildcare
                : 0.0;
        double rsuValue = (rsuAmount != null && rsuAmount > 0) ? rsuAmount : 0.0;

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
                MAX_401K_CONTRIBUTION_2024);

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
        double standardDeduction = married ? STANDARD_DEDUCTION_MARRIED_2025 : STANDARD_DEDUCTION_SINGLE_2025;
        double federalTaxableIncome = Math.max(0, taxableIncome - standardDeduction);
        double federalTaxOnSalary = calculateFederalTax(federalTaxableIncome, married);

        // RSU: Flat 22% supplemental rate (NOT subject to progressive brackets)
        double rsuFederalTax = rsuValue * RSU_SUPPLEMENTAL_FEDERAL_RATE;
        double totalFederalTax = federalTaxOnSalary + rsuFederalTax;

        // ============================================
        // Step D: State Tax
        // ============================================
        // State tax is calculated on taxable income (base salary) + RSU
        // RSU is usually taxed at the same rate as ordinary income in most states
        double stateTaxableIncome = taxableIncome + rsuValue;
        double stateTax = calculateStateTax(stateTaxableIncome, stateCode);

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
    public TaxBreakdown calculateTaxBreakdown(double salary, String stateCode) {
        double taxableIncome = Math.max(0, salary - STANDARD_DEDUCTION_SINGLE_2025);

        double federalTax = calculateFederalTax(taxableIncome, false); // Default to single for legacy
        double stateTax = calculateStateTax(salary, stateCode); // Legacy: uses gross income
        double ficaTax = calculateFICA(salary);

        TaxBreakdown breakdown = new TaxBreakdown();
        breakdown.grossIncome = salary;
        breakdown.standardDeduction = STANDARD_DEDUCTION_SINGLE_2025;
        breakdown.taxableIncome = taxableIncome;
        breakdown.federalTax = federalTax;
        breakdown.stateTax = stateTax;
        breakdown.socialSecurityTax = Math.min(salary, SOCIAL_SECURITY_CAP) * SOCIAL_SECURITY_RATE;
        breakdown.medicareTax = salary * MEDICARE_RATE;
        breakdown.additionalMedicareTax = salary > ADDITIONAL_MEDICARE_THRESHOLD_SINGLE
                ? (salary - ADDITIONAL_MEDICARE_THRESHOLD_SINGLE) * ADDITIONAL_MEDICARE_RATE
                : 0;
        breakdown.totalTax = federalTax + stateTax + ficaTax;
        breakdown.netIncome = Math.max(0, salary - breakdown.totalTax);

        return breakdown;
    }

    /**
     * Calculate Federal Tax using 2025 IRS brackets
     */
    /**
     * Calculate Federal Tax using 2025 IRS brackets
     */
    private double calculateFederalTax(double taxableIncome, boolean isMarried) {
        double tax = 0;
        double previousBracket = 0;
        double[][] brackets = isMarried ? FEDERAL_BRACKETS_2025_MARRIED : FEDERAL_BRACKETS_2025_SINGLE;

        for (double[] bracket : brackets) {
            double cap = bracket[0];
            double rate = bracket[1];

            if (taxableIncome <= previousBracket) {
                break;
            }

            double taxableInThisBracket = Math.min(taxableIncome, cap) - previousBracket;
            tax += taxableInThisBracket * rate;

            previousBracket = cap;
        }

        return tax;
    }

    /**
     * Calculate State Tax with strategy pattern
     * 
     * @param taxableIncome Taxable income (after pre-tax deductions)
     * @param stateCode     State code
     * @return State tax amount
     */
    private double calculateStateTax(double taxableIncome, String stateCode) {
        String state = stateCode.toUpperCase(Locale.US);

        // No Tax States
        for (String noTaxState : NO_TAX_STATES) {
            if (state.equals(noTaxState)) {
                return 0;
            }
        }

        // Flat Tax States
        if (FLAT_TAX_STATES.containsKey(state)) {
            return taxableIncome * FLAT_TAX_STATES.get(state);
        }

        // Progressive Tax States
        return calculateProgressiveStateTax(taxableIncome, state);
    }

    /**
     * Calculate Progressive State Tax (CA, NY, etc.)
     * 
     * @param taxableIncome Taxable income (after pre-tax deductions)
     * @param state         State code
     * @return State tax amount
     */
    private double calculateProgressiveStateTax(double taxableIncome, String state) {
        // California - Simplified progressive (effective rate approximation)
        if (state.equals("CA")) {
            if (taxableIncome <= 10_412)
                return taxableIncome * 0.01;
            if (taxableIncome <= 24_684)
                return taxableIncome * 0.02;
            if (taxableIncome <= 38_959)
                return taxableIncome * 0.04;
            if (taxableIncome <= 54_081)
                return taxableIncome * 0.06;
            if (taxableIncome <= 68_350)
                return taxableIncome * 0.08;
            if (taxableIncome <= 349_137)
                return taxableIncome * 0.093;
            if (taxableIncome <= 418_961)
                return taxableIncome * 0.103;
            if (taxableIncome <= 698_271)
                return taxableIncome * 0.113;
            return taxableIncome * 0.123;
        }

        // New York (State + NYC if applicable)
        if (state.equals("NY")) {
            double nyStateTax = calculateNYStateTax(taxableIncome);
            // Assume NYC for simplicity (add NYC tax)
            double nycTax = taxableIncome * 0.038; // ~3.8% NYC local tax
            return nyStateTax + nycTax;
        }

        // New Jersey
        if (state.equals("NJ")) {
            if (taxableIncome <= 20_000)
                return taxableIncome * 0.014;
            if (taxableIncome <= 35_000)
                return taxableIncome * 0.0175;
            if (taxableIncome <= 40_000)
                return taxableIncome * 0.035;
            if (taxableIncome <= 75_000)
                return taxableIncome * 0.05525;
            if (taxableIncome <= 500_000)
                return taxableIncome * 0.0637;
            if (taxableIncome <= 1_000_000)
                return taxableIncome * 0.0897;
            return taxableIncome * 0.1075;
        }

        // Fallback: Use repository data if available
        Map<String, StateTax> stateTaxMap = repository.stateTaxMap();
        StateTax stateTax = stateTaxMap.get(state);
        if (stateTax != null) {
            return computeTaxFromBrackets(taxableIncome, stateTax.getBrackets());
        }

        // Default: Assume moderate state tax (~5%)
        return taxableIncome * 0.05;
    }

    /**
     * Calculate NY State Tax
     * 
     * @param taxableIncome Taxable income (after pre-tax deductions)
     * @return NY State tax amount
     */
    private double calculateNYStateTax(double taxableIncome) {
        if (taxableIncome <= 8_500)
            return taxableIncome * 0.04;
        if (taxableIncome <= 11_700)
            return taxableIncome * 0.045;
        if (taxableIncome <= 13_900)
            return taxableIncome * 0.0525;
        if (taxableIncome <= 80_650)
            return taxableIncome * 0.055;
        if (taxableIncome <= 215_400)
            return taxableIncome * 0.06;
        if (taxableIncome <= 1_077_550)
            return taxableIncome * 0.0685;
        if (taxableIncome <= 5_000_000)
            return taxableIncome * 0.0965;
        if (taxableIncome <= 25_000_000)
            return taxableIncome * 0.103;
        return taxableIncome * 0.109;
    }

    /**
     * Calculate FICA (2025)
     * - Social Security: 6.2% up to $176,100
     * - Medicare: 1.45% on all earnings
     * - Additional Medicare: +0.9% over $200,000 (Single) / $250,000 (Married)
     * 
     * @param ficaTaxableBase Income base for FICA calculation (gross - pre-tax
     *                        insurance)
     * @param isMarried       true for Married filing jointly, false for Single
     * @return Total FICA tax
     */
    private double calculateFICAWithMaritalStatus(double ficaTaxableBase, boolean isMarried) {
        // Social Security: 6.2% up to cap
        double socialSecurity = Math.min(ficaTaxableBase, SOCIAL_SECURITY_CAP) * SOCIAL_SECURITY_RATE;

        // Medicare: 1.45% on all earnings (no cap)
        double medicare = ficaTaxableBase * MEDICARE_RATE;

        // Additional Medicare Tax (0.9% over threshold)
        double additionalMedicareThreshold = isMarried
                ? ADDITIONAL_MEDICARE_THRESHOLD_MARRIED
                : ADDITIONAL_MEDICARE_THRESHOLD_SINGLE;
        double additionalMedicare = 0;
        if (ficaTaxableBase > additionalMedicareThreshold) {
            additionalMedicare = (ficaTaxableBase - additionalMedicareThreshold) * ADDITIONAL_MEDICARE_RATE;
        }

        return socialSecurity + medicare + additionalMedicare;
    }

    /**
     * Calculate FICA (2025) - Legacy method for backward compatibility
     * - Social Security: 6.2% up to $176,100
     * - Medicare: 1.45% on all earnings
     * - Additional Medicare: +0.9% over $200,000
     */
    private double calculateFICA(double salary) {
        return calculateFICAWithMaritalStatus(salary, false); // Default to Single
    }

    /**
     * Legacy method for repository-based tax calculation
     */
    private double computeTaxFromBrackets(double salary, List<TaxBracket> brackets) {
        double tax = 0;
        double previous = 0;
        for (TaxBracket bracket : brackets) {
            Double cap = bracket.getUpTo();
            double taxable;
            if (cap == null) {
                taxable = Math.max(0, salary - previous);
            } else {
                taxable = Math.max(0, Math.min(salary, cap) - previous);
            }
            tax += taxable * bracket.getRate();
            if (cap == null || salary <= cap) {
                break;
            }
            previous = cap;
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
     * Supports Waterfall method: Gross -> Pre-tax Deductions -> FICA -> Taxable
     * Income -> Net
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
