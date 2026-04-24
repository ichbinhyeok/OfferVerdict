package com.offerverdict.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class LandingController {
    @GetMapping("/should-i-take-this-offer")
    public RedirectView shouldITakeThisOffer() {
        return permanentRedirect("/nurse-relocation-offer-checker");
    }

    @GetMapping("/job-offer-comparison-calculator")
    public RedirectView jobOfferComparisonCalculator() {
        return permanentRedirect("/nurse-relocation-offer-checker");
    }

    @GetMapping("/relocation-salary-calculator")
    public RedirectView relocationSalaryCalculator() {
        return permanentRedirect("/nurse-relocation-offer-checker");
    }

    @GetMapping("/is-this-salary-enough")
    public RedirectView isThisSalaryEnough() {
        return permanentRedirect("/nurse-relocation-offer-checker");
    }

    private RedirectView permanentRedirect(String destination) {
        RedirectView redirectView = new RedirectView(destination, true);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirectView;
    }
}
