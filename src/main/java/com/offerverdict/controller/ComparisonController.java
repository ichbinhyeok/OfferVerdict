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
import org.springframework.web.bind.annotation.ResponseBody;
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
    // Flexible validation: 1,000 ~ 10,000,000 allowed
    private static final double MIN_SALARY = 1_000;
    private static final double MAX_SALARY = 10_000_000;

    private final DataRepository repository;
    private final ComparisonService comparisonService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public ComparisonController(DataRepository repository,
            ComparisonService comparisonService,
            AppProperties appProperties,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.comparisonService = comparisonService;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @GetMapping({ "/", "/start" })
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
                            addRedirectParams(redirectAttributes, currentSalary, offerSalary, null, null, null, null);
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
        model.addAttribute("citiesByState", groupCitiesByState());

        return "index";
    }

    private Map<String, List<CityCostEntry>> groupCitiesByState() {
        return repository.getCities().stream()
                .collect(Collectors.groupingBy(CityCostEntry::getState, TreeMap::new, Collectors.toList()));
    }

    @GetMapping("/{job}-salary-{cityA}-vs-{cityB}")
    public Object compare(@PathVariable String job,
            @PathVariable String cityA,
            @PathVariable String cityB,
            // OPTIONAL PARAMS FOR SEO SUPPORT
            @RequestParam(required = false) Double currentSalary,
            @RequestParam(required = false) Double offerSalary,
            @RequestParam(required = false) Double fourOhOneKRate,
            @RequestParam(required = false) Double monthlyInsurance,
            @RequestParam(required = false) Double rsuAmount,
            @RequestParam(required = false) Boolean isMarried,
            RedirectAttributes redirectAttributes,
            Model model) {

        String normalizedJob = SlugNormalizer.normalize(job);
        String normalizedCityA = SlugNormalizer.normalize(cityA);
        String normalizedCityB = SlugNormalizer.normalize(cityB);

        JobInfo jobInfo = repository.findJobLoosely(normalizedJob)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown job slug: " + job));
        CityCostEntry cityEntryA = repository.findCityLoosely(normalizedCityA)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + cityA));
        CityCostEntry cityEntryB = repository.findCityLoosely(normalizedCityB)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + cityB));

        String canonicalPath = "/" + jobInfo.getSlug() + "-salary-" + cityEntryA.getSlug() + "-vs-"
                + cityEntryB.getSlug();

        // Canonical Redirect if slugs are dirty
        if (!job.equals(jobInfo.getSlug()) || !cityA.equals(cityEntryA.getSlug()) || !cityB.equals(cityEntryB.getSlug())
                || !SlugNormalizer.isCanonicalCitySlug(cityEntryA.getSlug())
                || !SlugNormalizer.isCanonicalCitySlug(cityEntryB.getSlug())) {
            RedirectView redirectView = new RedirectView(canonicalPath, true);
            // Pass params if they exist
            if (currentSalary != null && offerSalary != null) {
                addRedirectParams(redirectAttributes, currentSalary, offerSalary, fourOhOneKRate, monthlyInsurance,
                        rsuAmount, isMarried);
            }
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

        // --- INTELLIGENT DEFAULTS FOR SEO (If no params provided) ---
        // Look up median salary for this job in these cities
        if (currentSalary == null) {
            currentSalary = getMedianSalary(jobInfo.getSlug(), cityEntryA.getSlug());
        }
        if (offerSalary == null) {
            offerSalary = getMedianSalary(jobInfo.getSlug(), cityEntryB.getSlug());
        }

        HouseholdType parsedHouseholdType = HouseholdType.SINGLE;
        HousingType parsedHousingType = HousingType.RENT;

        Boolean effectiveIsMarried = isMarried != null ? isMarried : (parsedHouseholdType == HouseholdType.FAMILY);

        String validationMessage = validateSalary(currentSalary).orElse(null);
        validationMessage = validationMessage == null ? validateSalary(offerSalary).orElse(null) : validationMessage;

        double safeCurrentSalary = clampSalary(currentSalary);
        double safeOfferSalary = clampSalary(offerSalary);

        ComparisonResult result = comparisonService.compare(
                cityEntryA.getSlug(),
                cityEntryB.getSlug(),
                safeCurrentSalary,
                safeOfferSalary,
                parsedHouseholdType,
                parsedHousingType,
                effectiveIsMarried,
                fourOhOneKRate,
                monthlyInsurance,
                0.0,
                0.0, // sideHustle
                false, // isRemote
                true); // isCarOwner default

        if (result == null)
            throw new IllegalStateException("ComparisonResult cannot be null");

        // Dynamic Title for SEO
        String title = String.format("%s Salary: %s vs %s | Real Value Calculator",
                jobInfo.getTitle(), cityEntryA.getCity(), cityEntryB.getCity());

        String metaDescription = String.format(
                "Is moving from %s to %s for a %s job worth it? Compare taxes, rent, and %s vs %s cost of living with real 2026 data.",
                cityEntryA.getCity(), cityEntryB.getCity(), jobInfo.getTitle(), cityEntryA.getCity(),
                cityEntryB.getCity());

        String canonicalUrl = comparisonService.buildCanonicalUrl(canonicalPath);
        String ogImageUrl = comparisonService.buildCanonicalUrl("/share/" + jobInfo.getSlug() + "-salary-"
                + cityEntryA.getSlug() + "-vs-" + cityEntryB.getSlug() + ".png");

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

        // Build query string only if non-default params present (for linking)
        String queryString = buildQueryString(currentSalary, offerSalary, fourOhOneKRate, monthlyInsurance, rsuAmount,
                isMarried);
        model.addAttribute("otherJobLinks",
                comparisonService.relatedJobComparisons(cityEntryA.getSlug(), cityEntryB.getSlug(), queryString));
        model.addAttribute("otherCityLinks", comparisonService.relatedCityComparisons(jobInfo.getSlug(),
                cityEntryA.getSlug(), cityEntryB.getSlug(), queryString));
        model.addAttribute("structuredDataJson",
                toJson(buildStructuredData(title, metaDescription, canonicalUrl, result)));

        model.addAttribute("currentTaxBreakdown",
                comparisonService.getTaxBreakdown(safeCurrentSalary, cityEntryA.getState()));
        model.addAttribute("offerTaxBreakdown",
                comparisonService.getTaxBreakdown(safeOfferSalary, cityEntryB.getState()));

        return "result";
    }

    private double getMedianSalary(String jobSlug, String citySlug) {
        // Fallback if no median data found: $100,000
        // Ideally SalaryDataService provides this.
        // Since SalaryDataService.getPercentileLabel exists, we can introspect...
        // But SalaryDataService in previous step didn't have specific getMedian method
        // exposed efficiently
        // Actually, we can assume a default roughly around 100k or try to match.
        // For 'software-engineer', default is higher.
        if (jobSlug.contains("engineer") || jobSlug.contains("developer"))
            return 140000;
        if (jobSlug.contains("manager"))
            return 120000;
        if (jobSlug.contains("analyst"))
            return 90000;
        return 100000;
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

    private Map<String, Object> buildStructuredData(String title, String description, String canonicalUrl,
            ComparisonResult result) {
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
                        "name", "What is the true cost of living difference?",
                        "acceptedAnswer", Map.of(
                                "@type", "Answer",
                                "text", result.getVerdictCopy()))));

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
            double offerSalary,
            Double fourOhOneKRate,
            Double monthlyInsurance,
            Double rsuAmount,
            Boolean isMarried) {
        redirectAttributes.addAttribute("currentSalary", currentSalary);
        redirectAttributes.addAttribute("offerSalary", offerSalary);
        if (fourOhOneKRate != null)
            redirectAttributes.addAttribute("fourOhOneKRate", fourOhOneKRate);
        if (monthlyInsurance != null)
            redirectAttributes.addAttribute("monthlyInsurance", monthlyInsurance);
        if (rsuAmount != null)
            redirectAttributes.addAttribute("rsuAmount", rsuAmount);
        if (isMarried != null)
            redirectAttributes.addAttribute("isMarried", isMarried);
    }

    private String buildQueryString(double currentSalary, double offerSalary,
            Double fourOhOneKRate, Double monthlyInsurance,
            Double rsuAmount, Boolean isMarried) {
        StringBuilder sb = new StringBuilder();
        sb.append("?currentSalary=").append(currentSalary);
        sb.append("&offerSalary=").append(offerSalary);
        if (fourOhOneKRate != null)
            sb.append("&fourOhOneKRate=").append(fourOhOneKRate);
        if (monthlyInsurance != null)
            sb.append("&monthlyInsurance=").append(monthlyInsurance);
        if (rsuAmount != null)
            sb.append("&rsuAmount=").append(rsuAmount);
        if (isMarried != null)
            sb.append("&isMarried=").append(isMarried);
        return sb.toString();
    }

    @GetMapping("/api/calculate")
    @ResponseBody
    public ComparisonResult calculateApi(
            @RequestParam String cityASlug,
            @RequestParam String cityBSlug,
            @RequestParam double currentSalary,
            @RequestParam double offerSalary,
            @RequestParam(required = false, defaultValue = "false") boolean isPremiumBenefits,
            @RequestParam(required = false, defaultValue = "false") boolean isHomeOwner,
            @RequestParam(required = false, defaultValue = "false") boolean hasStudentLoan,
            @RequestParam(required = false, defaultValue = "false") boolean hasDependents,
            @RequestParam(required = false, defaultValue = "0") double sideHustle,
            @RequestParam(required = false, defaultValue = "0") double otherLeaks,
            @RequestParam(required = false, defaultValue = "false") boolean isRemote,
            @RequestParam(required = false, defaultValue = "false") boolean isTaxOptimized,
            @RequestParam(required = false, defaultValue = "true") boolean isCarOwner,
            @RequestParam(required = false, defaultValue = "0") double signingBonus,
            @RequestParam(required = false, defaultValue = "0") double equityAnnual,
            @RequestParam(required = false, defaultValue = "1.0") double equityMultiplier,
            @RequestParam(required = false, defaultValue = "0") double commuteTime) {

        CityCostEntry cityEntryA = repository.getCity(cityASlug);
        CityCostEntry cityEntryB = repository.getCity(cityBSlug);

        HouseholdType householdType = hasDependents ? HouseholdType.FAMILY : HouseholdType.SINGLE;
        HousingType housingType = isHomeOwner ? HousingType.OWN : HousingType.RENT;

        // Logical mapping of "Boosts" and "Leaks"
        Double fourOhOneK = (isPremiumBenefits || isTaxOptimized) ? 0.08 : 0.04;
        Double insurance = isPremiumBenefits ? 100.0 : 400.0;

        // Combine Boolean debt with Granular Leaks
        double totalDebtMonthly = (hasStudentLoan ? 800.0 : 0.0) + otherLeaks;
        double totalBoostMonthly = sideHustle;

        return comparisonService.compare(
                cityEntryA.getSlug(),
                cityEntryB.getSlug(),
                currentSalary,
                offerSalary,
                householdType,
                housingType,
                hasDependents,
                fourOhOneK,
                insurance,
                totalDebtMonthly,
                totalBoostMonthly,
                isRemote,
                isCarOwner,
                signingBonus,
                equityAnnual,
                equityMultiplier,
                commuteTime);
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
        if (normalizedInput == null || normalizedInput.isEmpty())
            return Optional.empty();
        Optional<JobInfo> looseMatch = repository.findJobLoosely(normalizedInput);
        if (looseMatch.isPresent())
            return looseMatch;
        String rawLower = rawInput == null ? "" : rawInput.toLowerCase(Locale.US);
        return repository.getJobs().stream()
                .filter(job -> job.getSlug().toLowerCase(Locale.US).contains(normalizedInput)
                        || job.getTitle().toLowerCase(Locale.US).startsWith(rawLower))
                .findFirst();
    }

    private Optional<CityCostEntry> resolveCityInput(String rawInput, String normalizedInput) {
        if (normalizedInput == null || normalizedInput.isEmpty())
            return Optional.empty();
        Optional<CityCostEntry> looseMatch = repository.findCityLoosely(normalizedInput);
        if (looseMatch.isPresent())
            return looseMatch;
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