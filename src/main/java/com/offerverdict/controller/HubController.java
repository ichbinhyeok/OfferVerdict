package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HubController {
    private final DataRepository repository;

    public HubController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/jobs")
    public String jobs(Model model) {
        List<JobInfo> jobs = repository.getJobs();
        model.addAttribute("jobs", jobs);
        model.addAttribute("title", "Job salary verdict hubs");
        model.addAttribute("metaDescription", "Explore verdict-first salary comparisons by job title.");
        return "jobs";
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
