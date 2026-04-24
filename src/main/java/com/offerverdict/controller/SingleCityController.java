package com.offerverdict.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class SingleCityController {

    @GetMapping("/salary-check/{citySlug}/{salaryInt}")
    public RedirectView singleCityAnalysis(@PathVariable("citySlug") String citySlug,
            @PathVariable("salaryInt") int salaryInt) {
        return permanentRedirect();
    }

    @GetMapping("/salary-check/{jobSlug}/{citySlug}/{salaryInt}")
    public RedirectView singleCityJobAnalysis(@PathVariable("jobSlug") String jobSlug,
            @PathVariable("citySlug") String citySlug,
            @PathVariable("salaryInt") int salaryInt) {
        return permanentRedirect();
    }

    private RedirectView permanentRedirect() {
        RedirectView redirectView = new RedirectView("/nurse-relocation-offer-checker", true);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirectView;
    }
}
