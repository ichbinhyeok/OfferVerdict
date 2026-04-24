package com.offerverdict.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class HubController {

    @GetMapping("/cities")
    public RedirectView cities() {
        return permanentRedirect();
    }

    @GetMapping("/city/{citySlug}")
    public RedirectView cityHub(@PathVariable String citySlug) {
        return permanentRedirect();
    }

    @GetMapping("/job/{jobSlug}")
    public RedirectView jobHub(@PathVariable String jobSlug) {
        return permanentRedirect();
    }

    private RedirectView permanentRedirect() {
        RedirectView redirectView = new RedirectView("/nurse-relocation-offer-checker", true);
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return redirectView;
    }
}
