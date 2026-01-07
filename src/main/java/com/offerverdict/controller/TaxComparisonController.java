package com.offerverdict.controller;

import com.offerverdict.service.ComparisonService;
import com.offerverdict.service.TaxCalculatorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TaxComparisonController {

    private final TaxCalculatorService taxCalculatorService;
    private final ComparisonService comparisonService;

    public TaxComparisonController(TaxCalculatorService taxCalculatorService, ComparisonService comparisonService) {
        this.taxCalculatorService = taxCalculatorService;
        this.comparisonService = comparisonService;
    }

    @GetMapping("/taxes/{stateA}-vs-{stateB}")
    public String compareTaxes(@PathVariable String stateA,
            @PathVariable String stateB,
            @RequestParam(defaultValue = "100000") double salary,
            Model model) {

        String normStateA = stateA.toUpperCase().length() > 2 ? stateA : stateA.toUpperCase();
        String normStateB = stateB.toUpperCase().length() > 2 ? stateB : stateB.toUpperCase();

        TaxCalculatorService.TaxResult resultA = taxCalculatorService.calculateTax(salary, normStateA, false, null,
                null, null, null);
        TaxCalculatorService.TaxResult resultB = taxCalculatorService.calculateTax(salary, normStateB, false, null,
                null, null, null);

        model.addAttribute("stateA", normStateA);
        model.addAttribute("stateB", normStateB);
        model.addAttribute("salary", salary);
        model.addAttribute("resultA", resultA);
        model.addAttribute("resultB", resultB);

        double diff = resultB.getNetIncome() - resultA.getNetIncome();
        model.addAttribute("diff", diff);
        model.addAttribute("title", String.format("%s vs %s Income Tax Calculator 2026", normStateA, normStateB));

        // Canonical structure
        String path = "/taxes/" + stateA + "-vs-" + stateB;
        model.addAttribute("canonicalUrl", comparisonService.buildCanonicalUrl(path));

        return "tax_comparison"; // Need to create simple tax_comparison.html or reuse result
    }
}
