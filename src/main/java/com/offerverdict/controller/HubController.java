package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
                .sorted(Comparator.comparing(CityCostEntry::getCity))
                .toList();
        model.addAttribute("cities", cities);
        model.addAttribute("title", "City-to-city salary tradeoffs");
        model.addAttribute("metaDescription", "See which cities stretch your paycheck with our verdict engine.");
        return "cities";
    }
}
