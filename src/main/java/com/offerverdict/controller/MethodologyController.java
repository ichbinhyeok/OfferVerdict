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
        model.addAttribute("title", "Methodology | OfferVerdict Healthcare Offer Risk");
        model.addAttribute("metaDescription",
                "How OfferVerdict estimates healthcare relocation offer risk using pay terms, taxes, rent, moving costs, shift differentials, and bonus repayment exposure.");
        model.addAttribute("lastTaxUpdate", "January 2026");
        model.addAttribute("lastCostUpdate", java.time.LocalDate.now());
        return "methodology";
    }
}
