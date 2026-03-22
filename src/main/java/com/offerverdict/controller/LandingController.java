package com.offerverdict.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping("/should-i-take-this-offer")
    public String shouldITakeThisOffer(Model model) {
        model.addAttribute("title", "Should I Take This Job Offer? | OfferVerdict");
        model.addAttribute("metaDescription",
                "Use OfferVerdict to compare two job offers after tax, rent, and cost of living. Get a practical verdict before you relocate or negotiate.");
        model.addAttribute("canonicalUrl", "https://livingcostcheck.com/should-i-take-this-offer");
        return "offer-decision";
    }

    @GetMapping("/job-offer-comparison-calculator")
    public String jobOfferComparisonCalculator(Model model) {
        model.addAttribute("title", "Job Offer Comparison Calculator | OfferVerdict");
        model.addAttribute("metaDescription",
                "Compare two job offers with taxes, rent, and cost of living included. See which offer leaves you with more real monthly cash flow.");
        model.addAttribute("canonicalUrl", "https://livingcostcheck.com/job-offer-comparison-calculator");
        model.addAttribute("landingTitle", "Job offer comparison calculator");
        model.addAttribute("landingEyebrow", "Calculator");
        model.addAttribute("landingLead",
                "Compare two offers when the city, salary, and monthly expenses all change at once.");
        model.addAttribute("landingBullets", java.util.List.of(
                "Compare current vs offer salary after tax",
                "Include rent and living-cost differences",
                "See monthly gain, loss, and negotiation threshold"));
        return "offer-decision";
    }

    @GetMapping("/relocation-salary-calculator")
    public String relocationSalaryCalculator(Model model) {
        model.addAttribute("title", "Relocation Salary Calculator | OfferVerdict");
        model.addAttribute("metaDescription",
                "Calculate whether moving for a job actually improves your finances after tax, rent, and cost of living.");
        model.addAttribute("canonicalUrl", "https://livingcostcheck.com/relocation-salary-calculator");
        model.addAttribute("landingTitle", "Relocation salary calculator");
        model.addAttribute("landingEyebrow", "Relocation");
        model.addAttribute("landingLead",
                "Use this when you are deciding whether a move for work improves your real standard of living.");
        model.addAttribute("landingBullets", java.util.List.of(
                "Stress-test a move before you sign",
                "Compare rent burden across cities",
                "See whether the raise survives the relocation"));
        return "offer-decision";
    }

    @GetMapping("/is-this-salary-enough")
    public String isThisSalaryEnough(Model model) {
        model.addAttribute("title", "Is This Salary Enough? | OfferVerdict");
        model.addAttribute("metaDescription",
                "Check whether a salary is enough in a specific city after tax, rent, and baseline living costs.");
        model.addAttribute("canonicalUrl", "https://livingcostcheck.com/is-this-salary-enough");
        model.addAttribute("landingTitle", "Is this salary enough?");
        model.addAttribute("landingEyebrow", "Salary check");
        model.addAttribute("landingLead",
                "Use the salary reality check when the main question is whether a number is actually livable in your city.");
        model.addAttribute("landingBullets", java.util.List.of(
                "Estimate take-home pay",
                "Compare salary to housing costs",
                "Understand monthly residual, not just headline pay"));
        return "offer-decision";
    }
}
