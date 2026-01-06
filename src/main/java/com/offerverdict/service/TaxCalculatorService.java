package com.offerverdict.service;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.Fica;
import com.offerverdict.model.StateTax;
import com.offerverdict.model.TaxBracket;
import com.offerverdict.model.TaxData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TaxCalculatorService {
    private final DataRepository repository;

    public TaxCalculatorService(DataRepository repository) {
        this.repository = repository;
    }

    public double calculateNetAnnual(double salary, String stateCode) {
        TaxData taxData = repository.getTaxData();
        Map<String, StateTax> stateTaxMap = repository.stateTaxMap();
        
        // Apply Standard Deduction ($14,600 for Single filer 2025)
        double standardDeduction = 14600.0;
        double taxableIncome = Math.max(0, salary - standardDeduction);
        
        double federalTax = computeTax(taxableIncome, taxData.getFederal().getBrackets());
        double stateTax = computeTax(taxableIncome, stateTaxMap.getOrDefault(stateCode.toUpperCase(Locale.US), defaultZeroState()).getBrackets());
        double ficaTax = computeFica(salary, taxData.getFica());
        double totalTax = federalTax + stateTax + ficaTax;
        return Math.max(0, salary - totalTax);
    }

    private double computeFica(double salary, Fica fica) {
        double socialSecurity = Math.min(salary, fica.getSocialSecurityCap()) * fica.getSocialSecurityRate();
        double medicare = salary * fica.getMedicareRate();
        return socialSecurity + medicare;
    }

    private double computeTax(double salary, List<TaxBracket> brackets) {
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

    private StateTax defaultZeroState() {
        StateTax stateTax = new StateTax();
        stateTax.setState("NA");
        TaxBracket bracket = new TaxBracket();
        bracket.setRate(0);
        bracket.setUpTo(null);
        stateTax.setBrackets(List.of(bracket));
        return stateTax;
    }
}
