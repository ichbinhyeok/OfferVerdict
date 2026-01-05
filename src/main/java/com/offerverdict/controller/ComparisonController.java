package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.JobInfo;
import com.offerverdict.service.ComparisonService;
import com.offerverdict.util.MetaDescriptionUtil;
import com.offerverdict.util.SlugNormalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ComparisonController {
    private static final double MIN_SALARY = 30_000;
    private static final double MAX_SALARY = 1_000_000;

    private final DataRepository repository;
    private final ComparisonService comparisonService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public ComparisonController(DataRepository repository, ComparisonService comparisonService, AppProperties appProperties, ObjectMapper objectMapper) {
        this.repository = repository;
        this.comparisonService = comparisonService;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "OfferVerdict – relocation verdicts");
        model.addAttribute("metaDescription", "Punchy relocation verdicts. UX-only landing (noindex).");
        model.addAttribute("robots", "noindex");
        model.addAttribute("jobs", repository.getJobs());
        model.addAttribute("cities", repository.getCities());
        model.addAttribute("currentSalary", 120000);
        model.addAttribute("offerSalary", 150000);
        model.addAttribute("householdType", com.offerverdict.model.HouseholdType.SINGLE);
        model.addAttribute("housingType", com.offerverdict.model.HousingType.RENT);
        return "index";
    }

    @GetMapping("/start")
    public RedirectView start(@RequestParam String job,
                              @RequestParam String cityA,
                              @RequestParam String cityB,
                              @RequestParam double currentSalary,
                              @RequestParam double offerSalary,
                              @RequestParam(defaultValue = "SINGLE") String householdType,
                              @RequestParam(defaultValue = "RENT") String housingType) {
        String jobSlug = SlugNormalizer.normalize(job);
        String cityASlug = SlugNormalizer.normalize(cityA);
        String cityBSlug = SlugNormalizer.normalize(cityB);
        com.offerverdict.model.HouseholdType household = parseHousehold(householdType);
        com.offerverdict.model.HousingType housing = parseHousing(housingType);
        String target = "/" + jobSlug + "-salary-" + cityASlug + "-vs-" + cityBSlug
                + "?currentSalary=" + currentSalary
                + "&offerSalary=" + offerSalary
                + "&householdType=" + household.name()
                + "&housingType=" + housing.name();
        RedirectView rv = new RedirectView(target, true);
        rv.setStatusCode(HttpStatus.FOUND);
        return rv;
    }

    @GetMapping("/{job}-salary-{cityA}-vs-{cityB}")
    public Object compare(@PathVariable String job,
                          @PathVariable String cityA,
                          @PathVariable String cityB,
                          @RequestParam double currentSalary,
                          @RequestParam double offerSalary,
                          @RequestParam(required = false, defaultValue = "SINGLE") String householdType,
                          @RequestParam(required = false, defaultValue = "RENT") String housingType,
                          RedirectAttributes redirectAttributes,
                          Model model) {

        String normalizedJob = SlugNormalizer.normalize(job);
        String normalizedCityA = SlugNormalizer.normalize(cityA);
        String normalizedCityB = SlugNormalizer.normalize(cityB);
        com.offerverdict.model.HouseholdType household = parseHousehold(householdType);
        com.offerverdict.model.HousingType housing = parseHousing(housingType);

        JobInfo jobInfo = repository.getJob(normalizedJob);
        CityCostEntry cityEntryA = repository.getCity(normalizedCityA);
        CityCostEntry cityEntryB = repository.getCity(normalizedCityB);

        String canonicalPath = "/" + jobInfo.getSlug() + "-salary-" + cityEntryA.getSlug() + "-vs-" + cityEntryB.getSlug();
        if (!job.equals(jobInfo.getSlug()) || !cityA.equals(cityEntryA.getSlug()) || !cityB.equals(cityEntryB.getSlug())
                || !SlugNormalizer.isCanonicalCitySlug(cityEntryA.getSlug()) || !SlugNormalizer.isCanonicalCitySlug(cityEntryB.getSlug())) {
            RedirectView redirectView = new RedirectView(canonicalPath, true);
            redirectAttributes.addAttribute("currentSalary", currentSalary);
            redirectAttributes.addAttribute("offerSalary", offerSalary);
            redirectAttributes.addAttribute("householdType", household.name());
            redirectAttributes.addAttribute("housingType", housing.name());
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

        String validationError = validateSalary(currentSalary, offerSalary);

        ComparisonResult result = comparisonService.compare(cityEntryA.getSlug(), cityEntryB.getSlug(),
                currentSalary, offerSalary, household, housing);

        String title = "Is $" + Math.round(offerSalary) + " in " + comparisonService.formatCityName(cityEntryB)
                + " better than $" + Math.round(currentSalary) + " in " + comparisonService.formatCityName(cityEntryA)
                + "? " + jobInfo.getTitle() + " Reality Check";

        String metaDescription = MetaDescriptionUtil.truncate(
                result.getVerdict().name().replace("_", "-") + " verdict: " + Math.round(result.getDeltaPercent()) + "% residual swing between "
                        + comparisonService.formatCityName(cityEntryA) + " and " + comparisonService.formatCityName(cityEntryB) + ".",
                155);

        String canonicalUrl = comparisonService.buildCanonicalUrl(canonicalPath);

        model.addAttribute("title", title);
        model.addAttribute("metaDescription", metaDescription);
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("ogTitle", title);
        model.addAttribute("ogDescription", metaDescription);
        model.addAttribute("ogUrl", canonicalUrl);
        model.addAttribute("job", jobInfo);
        model.addAttribute("cityA", cityEntryA);
        model.addAttribute("cityB", cityEntryB);
        model.addAttribute("jobsList", repository.getJobs());
        model.addAttribute("citiesList", repository.getCities());
        model.addAttribute("result", result);
        model.addAttribute("currentSalary", currentSalary);
        model.addAttribute("offerSalary", offerSalary);
        model.addAttribute("householdType", household);
        model.addAttribute("housingType", housing);
        model.addAttribute("validationError", validationError);
        model.addAttribute("otherJobLinks", comparisonService.relatedJobComparisons(cityEntryA.getSlug(), cityEntryB.getSlug(),
                currentSalary, offerSalary, household, housing));
        model.addAttribute("otherCityLinks", comparisonService.relatedCityComparisons(jobInfo.getSlug(), cityEntryA.getSlug(),
                currentSalary, offerSalary, household, housing));
        model.addAttribute("structuredDataJson", toJson(buildStructuredData(title, metaDescription, canonicalUrl, result)));
        double maxResidual = Math.max(Math.abs(result.getCurrent().getResidual()), Math.abs(result.getOffer().getResidual()));
        double residualRange = Math.max(1, maxResidual * 2);
        model.addAttribute("residualRange", residualRange);
        model.addAttribute("residualOffset", maxResidual);

        return "result";
    }

    private String validateSalary(double currentSalary, double offerSalary) {
        if (currentSalary < MIN_SALARY || currentSalary > MAX_SALARY || offerSalary < MIN_SALARY || offerSalary > MAX_SALARY) {
            return "Please enter a realistic salary.";
        }
        return null;
    }

    private Map<String, Object> buildStructuredData(String title, String description, String canonicalUrl, ComparisonResult result) {
        Map<String, Object> webpage = new HashMap<>();
        webpage.put("@context", "https://schema.org");
        webpage.put("@type", "WebPage");
        webpage.put("name", title);
        webpage.put("description", description);
        webpage.put("url", canonicalUrl);

        Map<String, Object> faq = new HashMap<>();
        faq.put("@context", "https://schema.org");
        faq.put("@type", "FAQPage");
        faq.put("mainEntity", List.of(
                Map.of(
                        "@type", "Question",
                        "name", "What does the verdict mean?",
                        "acceptedAnswer", Map.of(
                                "@type", "Answer",
                                "text", "We crunch taxes, rent, and baseline living costs to show the net monthly breathing room difference. Verdict: " + result.getVerdict()
                        )
                ),
                Map.of(
                        "@type", "Question",
                        "name", "Is this tax advice?",
                        "acceptedAnswer", Map.of(
                                "@type", "Answer",
                                "text", "No. This is an editorial verdict engine, not financial or tax advice."
                        )
                )
        ));

        Map<String, Object> data = new HashMap<>();
        data.put("@graph", List.of(webpage, faq));
        return data;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @GetMapping("/admin/reload-data")
    public ResponseEntity<String> reloadData() {
        if (!appProperties.isDevReloadEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Reload disabled");
        }
        repository.reload();
        return ResponseEntity.ok("Data reloaded");
    }

    private com.offerverdict.model.HouseholdType parseHousehold(String value) {
        return Optional.ofNullable(value)
                .map(String::toUpperCase)
                .flatMap(v -> {
                    try {
                        return Optional.of(com.offerverdict.model.HouseholdType.valueOf(v));
                    } catch (IllegalArgumentException ex) {
                        return Optional.empty();
                    }
                }).orElse(com.offerverdict.model.HouseholdType.SINGLE);
    }

    private com.offerverdict.model.HousingType parseHousing(String value) {
        return Optional.ofNullable(value)
                .map(String::toUpperCase)
                .flatMap(v -> {
                    try {
                        return Optional.of(com.offerverdict.model.HousingType.valueOf(v));
                    } catch (IllegalArgumentException ex) {
                        return Optional.empty();
                    }
                }).orElse(com.offerverdict.model.HousingType.RENT);
    }
}
