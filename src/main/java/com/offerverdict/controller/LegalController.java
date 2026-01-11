package com.offerverdict.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy Policy | OfferVerdict");
        model.addAttribute("metaDescription",
                "Privacy Policy for OfferVerdict. Learn how we collect, use, and protect your data.");
        // Noindex legal pages to keep search focus on content
        model.addAttribute("shouldIndex", false);
        return "privacy";
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("title", "Terms of Service | OfferVerdict");
        model.addAttribute("metaDescription",
                "Terms of Service for using OfferVerdict's salary and cost of living comparison tools.");
        // Noindex legal pages
        model.addAttribute("shouldIndex", false);
        return "terms";
    }
}
