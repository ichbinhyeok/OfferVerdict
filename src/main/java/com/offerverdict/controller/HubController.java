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

        // Also provide jobs for the directory
        var jobs = repository.getJobs();

        model.addAttribute("cities", cities);
        model.addAttribute("jobs", jobs);
        model.addAttribute("title", "OfferVerdict Directory: Cities & Jobs");
        model.addAttribute("metaDescription", "Browse cost of living analyses by city or job title.");
        return "cities";
    }

    // New Route: Simple Job Directory (Sitemap style, no complex ranking)
    @GetMapping("/job/{jobSlug}")
    public String jobHub(@PathVariable String jobSlug, Model model) {
        var job = repository.getJob(jobSlug);
        if (job == null)
            return "redirect:/cities";

        List<CityCostEntry> cities = repository.getCities().stream()
                .filter(c -> c.getPriority() <= 2) // Priority cities only for the list
                .sorted(Comparator.comparing(CityCostEntry::getCity))
                .toList();

        model.addAttribute("job", job);
        model.addAttribute("cities", cities);
        model.addAttribute("title", job.getTitle() + " Salary & Cost of Living by City");
        model.addAttribute("metaDescription",
                "Compare " + job.getTitle() + " salaries and cost of living across major US cities.");

        return "job-directory";
    }
}
