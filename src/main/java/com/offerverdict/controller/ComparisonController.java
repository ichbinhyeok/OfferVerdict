package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.exception.ResourceNotFoundException;
import com.offerverdict.service.ContentEnrichmentService;
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
    private final ContentEnrichmentService contentEnrichmentService;

    public ComparisonController(DataRepository repository,
            ComparisonService comparisonService,
            AppProperties appProperties,
            ObjectMapper objectMapper,
            ContentEnrichmentService contentEnrichmentService) {
        this.repository = repository;
        this.comparisonService = comparisonService;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.contentEnrichmentService = contentEnrichmentService;
    }

    @GetMapping({ "/", "/start" })
    public Object home(@RequestParam(name = "job", required = false) String job,
            @RequestParam(name = "cityA", required = false) String cityA,
            @RequestParam(name = "cityB", required = false) String cityB,
            @RequestParam(name = "currentSalary", required = false) Double currentSalary,
            @RequestParam(name = "offerSalary", required = false) Double offerSalary,
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
    public Object compare(@PathVariable("job") String job,
            @PathVariable("cityA") String cityA,
            @PathVariable("cityB") String cityB,
            // OPTIONAL PARAMS FOR SEO SUPPORT
            @RequestParam(name = "currentSalary", required = false) Double currentSalary,
            @RequestParam(name = "offerSalary", required = false) Double offerSalary,
            @RequestParam(name = "fourOhOneKRate", required = false) Double fourOhOneKRate,
            @RequestParam(name = "monthlyInsurance", required = false) Double monthlyInsurance,
            @RequestParam(name = "rsuAmount", required = false) Double rsuAmount,
            @RequestParam(name = "isMarried", required = false) Boolean isMarried,
            RedirectAttributes redirectAttributes,
            Model model) {

        System.out.println("DEBUG: Entering compare method with job=" + job);
        String normalizedJob = SlugNormalizer.normalize(job);
        System.out.println("DEBUG: Normalized job=" + normalizedJob);
        String normalizedCityA = SlugNormalizer.normalize(cityA);
        String normalizedCityB = SlugNormalizer.normalize(cityB);

        JobInfo jobInfo = repository.findJobLoosely(normalizedJob)
                .orElseGet(() -> {
                    // Start of Custom Job Logic
                    System.out.println("DEBUG: Creating CUSTOM JOB for " + job);
                    JobInfo custom = new JobInfo();
                    custom.setTitle(job); // Use raw input as title
                    custom.setSlug(normalizedJob);
                    custom.setCategory("Custom");
                    return custom;
                });

        // City lookups must be strict
        CityCostEntry cityEntryA = repository.findCityLoosely(normalizedCityA)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + cityA));
        CityCostEntry cityEntryB = repository.findCityLoosely(normalizedCityB)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + cityB));

        // SEO Enhancement: Enforce alphabetical city order to prevent duplicate content
        // Example: austin-vs-dallas (canonical) vs dallas-vs-austin (redirect)
        boolean shouldSwapCities = cityEntryA.getSlug().compareTo(cityEntryB.getSlug()) > 0;
        if (shouldSwapCities) {
            // Swap cities and redirect to canonical URL
            String canonicalSwappedPath = "/" + jobInfo.getSlug() + "-salary-" + cityEntryB.getSlug() + "-vs-"
                    + cityEntryA.getSlug();
            RedirectView redirectView = new RedirectView(canonicalSwappedPath, true);
            // Swap salary params too
            if (currentSalary != null && offerSalary != null) {
                addRedirectParams(redirectAttributes, offerSalary, currentSalary, fourOhOneKRate, monthlyInsurance,
                        rsuAmount, isMarried);
            }
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

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

        // SEO Enhancement: Canonicalize salaries to $5K buckets to reduce
        // near-duplicates
        // Example: $102,500 -> $100,000 (canonical)
        double canonicalCurrentSalary = canonicalizeSalary(currentSalary);
        double canonicalOfferSalary = canonicalizeSalary(offerSalary);

        if (currentSalary != canonicalCurrentSalary || offerSalary != canonicalOfferSalary) {
            // Redirect to canonical salary bucket
            RedirectView redirectView = new RedirectView(canonicalPath, true);
            addRedirectParams(redirectAttributes, canonicalCurrentSalary, canonicalOfferSalary, fourOhOneKRate,
                    monthlyInsurance, rsuAmount, isMarried);
            redirectView.setStatusCode(HttpStatus.FOUND); // 302 for query param changes
            return redirectView;
        }

        // Ensure non-null for calculation
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

        // SEO Enhancement: Determine if this page should be indexed
        boolean shouldIndex = shouldIndexThisPage(jobInfo, cityEntryA, cityEntryB, safeCurrentSalary, safeOfferSalary);
        model.addAttribute("shouldIndex", shouldIndex);

        // SEO Enhancement: Add contextual content
        model.addAttribute("cityAContext", contentEnrichmentService.getCityContext(cityEntryA.getSlug()).orElse(null));
        model.addAttribute("cityBContext", contentEnrichmentService.getCityContext(cityEntryB.getSlug()).orElse(null));
        model.addAttribute("jobContext", contentEnrichmentService.getJobContext(jobInfo.getSlug()).orElse(null));

        return "result";
    }

    private double getMedianSalary(String jobSlug, String citySlug) {
        // --- REALISTIC SALARY ESTIMATES (2025/2026 Baseline) ---
        // These are national medians used when specific city data is missing

        // High Income (> $110k)
        if (jobSlug.contains("doctor") || jobSlug.contains("physician"))
            return 220000;
        if (jobSlug.contains("pilot"))
            return 130000;
        if (jobSlug.contains("lawyer") || jobSlug.contains("attorney"))
            return 140000;
        if (jobSlug.contains("manager") && (jobSlug.contains("product") || jobSlug.contains("engineering")))
            return 145000;
        if (jobSlug.contains("software") || jobSlug.contains("data-scientist"))
            return 135000;
        if (jobSlug.contains("pharmacist"))
            return 125000;

        // Upper Middle (> $80k)
        if (jobSlug.contains("manager"))
            return 95000; // General managers
        if (jobSlug.contains("project-manager"))
            return 105000;
        if (jobSlug.contains("physical-therapist"))
            return 95000;
        if (jobSlug.contains("ux-designer"))
            return 95000;
        if (jobSlug.contains("cybersecurity") || jobSlug.contains("devops"))
            return 115000;
        if (jobSlug.contains("engineer"))
            return 100000; // Generic engineer

        // Middle Income (> $60k)
        if (jobSlug.contains("nurse") || jobSlug.contains("rn"))
            return 82000;
        if (jobSlug.contains("dental-hygienist"))
            return 85000;
        if (jobSlug.contains("accountant") || jobSlug.contains("analyst"))
            return 75000;
        if (jobSlug.contains("police") || jobSlug.contains("firefighter"))
            return 70000;
        if (jobSlug.contains("teacher") || jobSlug.contains("professor"))
            return 65000;
        if (jobSlug.contains("electrician") || jobSlug.contains("plumber") || jobSlug.contains("leads"))
            return 65000;
        if (jobSlug.contains("hr-") || jobSlug.contains("marketing"))
            return 70000;

        // Skilled Trade / Admin (> $45k)
        if (jobSlug.contains("mechanic") || jobSlug.contains("hvac") || jobSlug.contains("welder"))
            return 55000;
        if (jobSlug.contains("carpenter") || jobSlug.contains("truck"))
            return 55000;
        if (jobSlug.contains("admin") || jobSlug.contains("office-manager"))
            return 52000;
        if (jobSlug.contains("sales-rep") || jobSlug.contains("real-estate"))
            return 60000; // Variable
        if (jobSlug.contains("paramedic"))
            return 50000;
        if (jobSlug.contains("graphic-designer"))
            return 55000;
        if (jobSlug.contains("social-worker"))
            return 58000;

        // Service / Entry (> $30k)
        if (jobSlug.contains("medical-assistant") || jobSlug.contains("dental-assistant"))
            return 38000;
        if (jobSlug.contains("customer-service"))
            return 42000;
        if (jobSlug.contains("warehouse"))
            return 38000;
        if (jobSlug.contains("bartender") || jobSlug.contains("server") || jobSlug.contains("waitstaff"))
            return 45000; // Including tips
        if (jobSlug.contains("retail") || jobSlug.contains("cook") || jobSlug.contains("chef"))
            return 35000;
        if (jobSlug.contains("stylist"))
            return 40000;

        return 75000; // Fallback National Median
    }

    /**
     * SEO Enhancement: Canonicalize salary to nearest $5K bucket.
     * Reduces near-duplicate pages (e.g., $102,500 vs $100,000).
     */
    private double canonicalizeSalary(double salary) {
        int bucket = 5000;
        return Math.round(salary / bucket) * bucket;
    }

    /**
     * SEO Enhancement: Determine if this page combination should be indexed.
     * Prevents indexing of low-value combinations to avoid thin content penalties.
     */
    private boolean shouldIndexThisPage(JobInfo job, CityCostEntry cityA, CityCostEntry cityB,
            double currentSalary, double offerSalary) {
        // Don't index if both cities are low-priority (Tier 3+)
        if (cityA.getTier() >= 3 && cityB.getTier() >= 3) {
            return false;
        }

        // Don't index if job is not "major" and both cities are not Tier 1
        if (!job.isMajor() && cityA.getTier() > 1 && cityB.getTier() > 1) {
            return false;
        }

        // Don't index extreme salary outliers (< $20k or > $500k)
        if (currentSalary < 20000 || currentSalary > 500000) {
            return false;
        }
        if (offerSalary < 20000 || offerSalary > 500000) {
            return false;
        }

        return true; // Index this page
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
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
            @RequestParam("cityASlug") String cityASlug,
            @RequestParam("cityBSlug") String cityBSlug,
            @RequestParam("currentSalary") double currentSalary,
            @RequestParam("offerSalary") double offerSalary,
            @RequestParam(name = "isPremiumBenefits", required = false, defaultValue = "false") boolean isPremiumBenefits,
            @RequestParam(name = "isHomeOwner", required = false, defaultValue = "false") boolean isHomeOwner,
            @RequestParam(name = "hasStudentLoan", required = false, defaultValue = "false") boolean hasStudentLoan,
            @RequestParam(name = "hasDependents", required = false, defaultValue = "false") boolean hasDependents,
            @RequestParam(name = "sideHustle", required = false, defaultValue = "0") double sideHustle,
            @RequestParam(name = "otherLeaks", required = false, defaultValue = "0") double otherLeaks,
            @RequestParam(name = "isRemote", required = false, defaultValue = "false") boolean isRemote,
            @RequestParam(name = "isTaxOptimized", required = false, defaultValue = "false") boolean isTaxOptimized,
            @RequestParam(name = "isCarOwner", required = false, defaultValue = "true") boolean isCarOwner,
            @RequestParam(name = "signingBonus", required = false, defaultValue = "0") double signingBonus,
            @RequestParam(name = "equityAnnual", required = false, defaultValue = "0") double equityAnnual,
            @RequestParam(name = "equityMultiplier", required = false, defaultValue = "1.0") double equityMultiplier,
            @RequestParam(name = "commuteTime", required = false, defaultValue = "0") double commuteTime) {

        CityCostEntry cityEntryA = repository.getCity(cityASlug);
        CityCostEntry cityEntryB = repository.getCity(cityBSlug);

        HouseholdType householdType = hasDependents ? HouseholdType.FAMILY : HouseholdType.SINGLE;
        HousingType housingType = isHomeOwner ? HousingType.OWN : HousingType.RENT;

        // Logical mapping of "Boosts" and "Leaks"
        Double fourOhOneK = (isPremiumBenefits || isTaxOptimized) ? 0.08 : 0.04;
        Double insurance = isPremiumBenefits ? 100.0 : 400.0;

        // Combine Boolean debt with Granular Leaks
        // FIXED: Separate "Other Leaks" from "Student Loan".
        // Student Loan is a shared reality (exists in both).
        // Other Leaks (Simulation Lab slider) is strictly an OFFER-side simulation
        // (e.g. lifestyle creep).
        double sharedDebtMonthly = (hasStudentLoan ? 800.0 : 0.0);
        double offerLeaksMonthly = otherLeaks;

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
                sharedDebtMonthly, // Shared Debt (Both)
                offerLeaksMonthly, // Offer Leaks (City B only)
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

        // Custom Fallback
        return Optional.of(new JobInfo(rawInput, normalizedInput, "Custom"));
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