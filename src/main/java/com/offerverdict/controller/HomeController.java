package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.HousingType;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.JobInfo;
import com.offerverdict.util.SlugNormalizer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {
    private final DataRepository repository;

    public HomeController(DataRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String index(Model model,
                        @RequestParam(value = "currentSalary", required = false) Double currentSalary,
                        @RequestParam(value = "offerSalary", required = false) Double offerSalary,
                        @RequestParam(value = "cityA", required = false) String cityA,
                        @RequestParam(value = "cityB", required = false) String cityB,
                        @RequestParam(value = "job", required = false) String job,
                        @RequestParam(value = "householdType", required = false) HouseholdType householdType,
                        @RequestParam(value = "housingType", required = false) HousingType housingType) {

        List<CityCostEntry> cities = repository.getCities().stream()
                .sorted(Comparator.comparing(CityCostEntry::getCity))
                .toList();
        List<JobInfo> jobs = repository.getJobs();

        CityCostEntry defaultCityA = cityA != null ? repository.getCity(SlugNormalizer.normalize(cityA)) : cities.get(0);
        CityCostEntry defaultCityB = cityB != null ? repository.getCity(SlugNormalizer.normalize(cityB)) : cities.get(Math.min(1, cities.size() - 1));
        JobInfo defaultJob = job != null ? repository.getJob(SlugNormalizer.normalize(job)) : jobs.get(0);

        model.addAttribute("cities", cities);
        model.addAttribute("jobs", jobs);
        model.addAttribute("currentSalary", currentSalary != null ? currentSalary : 90000);
        model.addAttribute("offerSalary", offerSalary != null ? offerSalary : 120000);
        model.addAttribute("cityASelected", defaultCityA.getSlug());
        model.addAttribute("cityBSelected", defaultCityB.getSlug());
        model.addAttribute("jobSelected", defaultJob.getSlug());
        model.addAttribute("householdType", householdType != null ? householdType : HouseholdType.SINGLE);
        model.addAttribute("housingType", housingType != null ? housingType : HousingType.RENT);
        model.addAttribute("title", "OfferVerdict | Modern relocation verdicts");
        model.addAttribute("metaDescription", "Compare paychecks between cities with tax, rent, and lifestyle adjustments baked in.");
        return "index";
    }

    @GetMapping("/start")
    public RedirectView start(@RequestParam double currentSalary,
                              @RequestParam double offerSalary,
                              @RequestParam String cityA,
                              @RequestParam String cityB,
                              @RequestParam String job,
                              @RequestParam(defaultValue = "SINGLE") HouseholdType householdType,
                              @RequestParam(defaultValue = "RENT") HousingType housingType,
                              RedirectAttributes redirectAttributes) {

        JobInfo jobInfo = repository.getJob(SlugNormalizer.normalize(job));
        CityCostEntry origin = repository.getCity(SlugNormalizer.normalize(cityA));
        CityCostEntry destination = repository.getCity(SlugNormalizer.normalize(cityB));

        String path = "/" + jobInfo.getSlug() + "-salary-" + origin.getSlug() + "-vs-" + destination.getSlug();

        redirectAttributes.addAttribute("currentSalary", currentSalary);
        redirectAttributes.addAttribute("offerSalary", offerSalary);
        redirectAttributes.addAttribute("householdType", householdType);
        redirectAttributes.addAttribute("housingType", housingType);

        RedirectView redirectView = new RedirectView(path, true);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
