package com.offerverdict.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContactController {

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About Us - OfferVerdict");
        model.addAttribute("metaDescription",
                "Learn why Gabi created OfferVerdict to help you make smarter relocation decisions.");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Contact Us - OfferVerdict");
        model.addAttribute("metaDescription", "Get in touch with the OfferVerdict team.");
        return "contact";
    }
}
