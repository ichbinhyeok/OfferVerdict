package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import com.offerverdict.util.SlugNormalizer;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;

@Controller
public class HomeController {

    private final DataRepository repository;

    public HomeController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("cities", repository.getCities().stream()
                .sorted(Comparator.comparing(CityCostEntry::getCity)).toList());
        model.addAttribute("jobs", repository.getJobs());
        model.addAttribute("title", "OfferVerdict - Salary Purchasing Power Calculator");
        model.addAttribute("metaDescription", "Don't just compare gross salaries. Compare your actual buying power.");
        return "index";
    }

    @GetMapping("/start")
    public String startComparison(
            @RequestParam String job,
            @RequestParam String cityA,
            @RequestParam String cityB,
            @RequestParam(defaultValue = "100000") double currentSalary,
            @RequestParam(defaultValue = "120000") double offerSalary,
            @RequestParam(defaultValue = "SINGLE") String householdType,
            @RequestParam(defaultValue = "RENT") String housingType) {

        String url = String.format("redirect:/%s-salary-%s-vs-%s?currentSalary=%.0f&offerSalary=%.0f&householdType=%s&housingType=%s",
                SlugNormalizer.normalize(job),
                SlugNormalizer.normalize(cityA),
                SlugNormalizer.normalize(cityB),
                currentSalary,
                offerSalary,
                householdType,
                housingType);

        return url;
    }
}