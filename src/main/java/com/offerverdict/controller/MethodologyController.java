package com.offerverdict.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

/**
 * Controller for methodology and transparency pages.
 * Critical for YMYL compliance and E-A-T signals.
 */
@Controller
public class MethodologyController {

    @GetMapping("/methodology")
    public String methodology(Model model) {
        model.addAttribute("title", "Our Methodology - OfferVerdict");
        model.addAttribute("metaDescription",
                "Learn how OfferVerdict calculates cost of living comparisons using IRS tax data, BLS statistics, and real-time market data.");
        model.addAttribute("lastTaxUpdate", "January 2026");
        model.addAttribute("lastCostUpdate", LocalDate.now());
        return "methodology";
    }
}
