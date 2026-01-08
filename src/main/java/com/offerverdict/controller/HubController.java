package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;

@Controller
public class HubController {
    private final DataRepository repository;

    public HubController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/cities")
    public String cities(Model model) {
        List<CityCostEntry> cities = repository.getCities().stream()
                .sorted(Comparator.comparing(CityCostEntry::getPriority) // Priority sort first
                        .thenComparing(CityCostEntry::getCity))
                .toList();
                
        model.addAttribute("cities", cities);
        model.addAttribute("title", "City-to-city salary tradeoffs");
        model.addAttribute("metaDescription", "See which cities stretch your paycheck with our verdict engine.");
        return "cities";
    }

    // New Route: SEO Hub for Jobs (Top Paying Cities for X)
    @GetMapping("/job/{jobSlug}")
    public String jobHub(@PathVariable String jobSlug, Model model) {
        // Just a placeholder routing for now to prevent 404s if linked
        return "redirect:/";
    }
}
