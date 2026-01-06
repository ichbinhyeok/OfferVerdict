package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.exception.ResourceNotFoundException;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
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
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Controller
public class ComparisonController {
    // 유연한 검증: 1,000 ~ 10,000,000 허용
    private static final double MIN_SALARY = 1_000;
    private static final double MAX_SALARY = 10_000_000;

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

    @GetMapping({"/", "/start"})
    public Object home(@RequestParam(required = false) String job,
                       @RequestParam(required = false) String cityA,
                       @RequestParam(required = false) String cityB,
                       @RequestParam(required = false) Double currentSalary,
                       @RequestParam(required = false) Double offerSalary,
                       RedirectAttributes redirectAttributes,
                       Model model) {

        if (job != null && cityA != null && cityB != null && currentSalary != null && offerSalary != null) {
            try {
                String normalizedJob = SlugNormalizer.normalize(job);
                String normalizedCityA = SlugNormalizer.normalize(cityA);
                String normalizedCityB = SlugNormalizer.normalize(cityB);

                Optional<JobInfo> jobInfo = resolveJobInput(job, normalizedJob);
                Optional<CityCostEntry> cityMatchA = resolveCityInput(cityA, normalizedCityA);
                Optional<CityCostEntry> cityMatchB = resolveCityInput(cityB, normalizedCityB);

                if (jobInfo.isPresent() && cityMatchA.isPresent() && cityMatchB.isPresent()) {
                    // Validate salaries
                    if (currentSalary < MIN_SALARY || currentSalary > MAX_SALARY ||
                        offerSalary < MIN_SALARY || offerSalary > MAX_SALARY) {
                        model.addAttribute("validationMessage", 
                            String.format("Salaries must be between $%,.0f and $%,.0f", MIN_SALARY, MAX_SALARY));
                    } else {
                        String targetPath = String.format("/%s-salary-%s-vs-%s",
                                jobInfo.get().getSlug(),
                                cityMatchA.get().getSlug(),
                                cityMatchB.get().getSlug());

                        if (targetPath != null) {
                            RedirectView redirectView = new RedirectView(targetPath, true);
                            redirectView.setStatusCode(HttpStatus.FOUND);
                            addRedirectParams(redirectAttributes, currentSalary, offerSalary);
                            return redirectView;
                        }
                    }
                } else {
                    model.addAttribute("validationMessage", 
                        "Could not find matching job or cities. Please check your inputs.");
                }
            } catch (Exception e) {
                model.addAttribute("validationMessage", 
                    "An error occurred. Please check your inputs and try again.");
            }
        }

        model.addAttribute("jobs", repository.getJobs());
        model.addAttribute("cities", repository.getCities());
        model.addAttribute("title", "OfferVerdict | Reality-check your job move");
        model.addAttribute("metaDescription", "Compare your actual buying power with taxes and cost of living.");
        model.addAttribute("jobsByCategory", groupJobsByCategory());

        return "index";
    }

    @GetMapping("/{job}-salary-{cityA}-vs-{cityB}")
    public Object compare(@PathVariable String job,
                          @PathVariable String cityA,
                          @PathVariable String cityB,
                          @RequestParam double currentSalary,
                          @RequestParam double offerSalary,
                          RedirectAttributes redirectAttributes,
                          Model model) {

        String normalizedJob = SlugNormalizer.normalize(job);
        String normalizedCityA = SlugNormalizer.normalize(cityA);
        String normalizedCityB = SlugNormalizer.normalize(cityB);

        JobInfo jobInfo = repository.findJobLoosely(normalizedJob).orElseThrow(() -> new ResourceNotFoundException("Unknown job slug: " + job));
        CityCostEntry cityEntryA = repository.findCityLoosely(normalizedCityA).orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + cityA));
        CityCostEntry cityEntryB = repository.findCityLoosely(normalizedCityB).orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + cityB));

        String canonicalPath = "/" + jobInfo.getSlug() + "-salary-" + cityEntryA.getSlug() + "-vs-" + cityEntryB.getSlug();
        if (!job.equals(jobInfo.getSlug()) || !cityA.equals(cityEntryA.getSlug()) || !cityB.equals(cityEntryB.getSlug())
                || !SlugNormalizer.isCanonicalCitySlug(cityEntryA.getSlug()) || !SlugNormalizer.isCanonicalCitySlug(cityEntryB.getSlug())) {
            RedirectView redirectView = new RedirectView(canonicalPath, true);
            addRedirectParams(redirectAttributes, currentSalary, offerSalary);
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

        // Always use default values - user can adjust in Life Simulator
        HouseholdType parsedHouseholdType = HouseholdType.SINGLE;
        HousingType parsedHousingType = HousingType.RENT;

        String validationMessage = validateSalary(currentSalary).orElse(null);
        validationMessage = validationMessage == null ? validateSalary(offerSalary).orElse(null) : validationMessage;

        double safeCurrentSalary = clampSalary(currentSalary);
        double safeOfferSalary = clampSalary(offerSalary);

        // Execute comparison and ensure result is not null
        ComparisonResult result = comparisonService.compare(cityEntryA.getSlug(), cityEntryB.getSlug(), safeCurrentSalary, safeOfferSalary, parsedHouseholdType, parsedHousingType);
        
        // Defensive null checks for result components
        if (result == null) {
            throw new IllegalStateException("ComparisonResult cannot be null");
        }
        if (result.getCurrent() == null || result.getOffer() == null) {
            throw new IllegalStateException("ComparisonResult.current and ComparisonResult.offer cannot be null");
        }

        String title = "Is $" + Math.round(offerSalary) + " in " + comparisonService.formatCityName(cityEntryB)
                + " better than $" + Math.round(currentSalary) + " in " + comparisonService.formatCityName(cityEntryA)
                + "? " + jobInfo.getTitle() + " Reality Check";

        String metaDescription = MetaDescriptionUtil.truncate(
                result.getVerdict().name().replace("_", "-") + " verdict: " + Math.round(result.getDeltaPercent()) + "% residual swing between "
                        + comparisonService.formatCityName(cityEntryA) + " and " + comparisonService.formatCityName(cityEntryB) + ".",
                155);

        String canonicalUrl = comparisonService.buildCanonicalUrl(canonicalPath);
        String ogImageUrl = comparisonService.buildCanonicalUrl("/share/" + jobInfo.getSlug() + "-salary-" + cityEntryA.getSlug() + "-vs-" + cityEntryB.getSlug() + ".png?currentSalary=" + Math.round(safeCurrentSalary) + "&offerSalary=" + Math.round(safeOfferSalary) + "&delta=" + Math.round(result.getDeltaPercent()) + "&verdict=" + result.getVerdict().name());

        // Safely add all attributes with guaranteed non-null values
        model.addAttribute("title", title);
        model.addAttribute("metaDescription", metaDescription);
        model.addAttribute("canonicalUrl", canonicalUrl);
        model.addAttribute("ogTitle", title);
        model.addAttribute("ogDescription", metaDescription);
        model.addAttribute("ogUrl", canonicalUrl);
        model.addAttribute("ogImageUrl", ogImageUrl);
        model.addAttribute("job", jobInfo);
        model.addAttribute("cityA", cityEntryA);
        model.addAttribute("cityB", cityEntryB);
        model.addAttribute("result", result);
        model.addAttribute("currentSalary", currentSalary);
        model.addAttribute("offerSalary", offerSalary);
        model.addAttribute("householdType", parsedHouseholdType);
        model.addAttribute("housingType", parsedHousingType);
        model.addAttribute("validationMessage", validationMessage);
        model.addAttribute("cities", repository.getCities());
        model.addAttribute("jobs", repository.getJobs());
        model.addAttribute("otherJobLinks", comparisonService.relatedJobComparisons(cityEntryA.getSlug(), cityEntryB.getSlug(), buildQueryString(currentSalary, offerSalary)));
        model.addAttribute("otherCityLinks", comparisonService.relatedCityComparisons(jobInfo.getSlug(), cityEntryA.getSlug(), cityEntryB.getSlug(), buildQueryString(currentSalary, offerSalary)));
        model.addAttribute("structuredDataJson", toJson(buildStructuredData(title, metaDescription, canonicalUrl, result)));

        return "result";
    }

    private double clampSalary(double salary) {
        return Math.min(Math.max(salary, MIN_SALARY), MAX_SALARY);
    }

    private Optional<String> validateSalary(double salary) {
        if (salary < MIN_SALARY || salary > MAX_SALARY) {
            return Optional.of("Please enter a realistic salary.");
        }
        return Optional.empty();
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

    private void addRedirectParams(RedirectAttributes redirectAttributes,
                                   double currentSalary,
                                   double offerSalary) {
        redirectAttributes.addAttribute("currentSalary", currentSalary);
        redirectAttributes.addAttribute("offerSalary", offerSalary);
    }

    private String buildQueryString(double currentSalary, double offerSalary) {
        return "?currentSalary=" + currentSalary + "&offerSalary=" + offerSalary;
    }

    @GetMapping("/admin/reload-data")
    public ResponseEntity<String> reloadData() {
        if (!appProperties.isDevReloadEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Reload disabled");
        }
        repository.reload();
        return ResponseEntity.ok("Data reloaded");
    }

    private Optional<JobInfo> resolveJobInput(String rawInput, String normalizedInput) {
        if (normalizedInput == null || normalizedInput.isEmpty()) {
            return Optional.empty();
        }

        Optional<JobInfo> looseMatch = repository.findJobLoosely(normalizedInput);
        if (looseMatch.isPresent()) {
            return looseMatch;
        }

        String rawLower = rawInput == null ? "" : rawInput.toLowerCase(Locale.US);
        return repository.getJobs().stream()
                .filter(job -> job.getSlug().toLowerCase(Locale.US).contains(normalizedInput)
                        || job.getTitle().toLowerCase(Locale.US).startsWith(rawLower))
                .findFirst();
    }

    private Optional<CityCostEntry> resolveCityInput(String rawInput, String normalizedInput) {
        if (normalizedInput == null || normalizedInput.isEmpty()) {
            return Optional.empty();
        }

        Optional<CityCostEntry> looseMatch = repository.findCityLoosely(normalizedInput);
        if (looseMatch.isPresent()) {
            return looseMatch;
        }

        String rawLower = rawInput == null ? "" : rawInput.toLowerCase(Locale.US);
        return repository.getCities().stream()
                .filter(city -> city.getSlug().toLowerCase(Locale.US).contains(normalizedInput)
                        || city.getCity().toLowerCase(Locale.US).startsWith(rawLower))
                .findFirst();
    }

    private Map<String, List<JobInfo>> groupJobsByCategory() {
        return repository.getJobs().stream()
                .collect(Collectors.groupingBy(JobInfo::getCategory, TreeMap::new, Collectors.toList()));
    }
}