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

import java.util.ArrayList;
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

    @GetMapping("/favicon.ico")
    public String favicon() {
        return "forward:/favicon.svg";
    }

    @GetMapping({ "/", "/start" })
    public Object home(@RequestParam(name = "job", required = false) String job,
            @RequestParam(name = "cityA", required = false) String cityA,
            @RequestParam(name = "cityB", required = false) String cityB,
            @RequestParam(name = "currentSalary", required = false) Double currentSalary,
            @RequestParam(name = "offerSalary", required = false) Double offerSalary,
            @RequestParam(name = "salaryType", required = false, defaultValue = "annual") String salaryType,
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
                    // Smart Validation: Account for monthly inputs
                    double currentMultiplier = ("monthly".equalsIgnoreCase(salaryType) || currentSalary < 12000) ? 12.0
                            : 1.0;
                    double offerMultiplier = ("monthly".equalsIgnoreCase(salaryType) || offerSalary < 12000) ? 12.0
                            : 1.0;

                    if (currentSalary * currentMultiplier < MIN_SALARY || currentSalary * currentMultiplier > MAX_SALARY
                            ||
                            offerSalary * offerMultiplier < MIN_SALARY || offerSalary * offerMultiplier > MAX_SALARY) {
                        model.addAttribute("validationMessage",
                                String.format("Salaries must be between $%,.0f and $%,.0f (Annualized)", MIN_SALARY,
                                        MAX_SALARY));
                    } else {
                        String targetPath = String.format("/%s-salary-%s-vs-%s",
                                jobInfo.get().getSlug(),
                                cityMatchA.get().getSlug(),
                                cityMatchB.get().getSlug());

                        if (targetPath != null) {
                            RedirectView redirectView = new RedirectView(targetPath, true);
                            redirectView.setStatusCode(HttpStatus.FOUND);
                            addRedirectParams(redirectAttributes, currentSalary, offerSalary, salaryType,
                                    null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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
            @RequestParam(name = "salaryType", required = false, defaultValue = "annual") String salaryType,
            @RequestParam(name = "fourOhOneKRate", required = false) Double fourOhOneKRate,
            @RequestParam(name = "monthlyInsurance", required = false) Double monthlyInsurance,
            @RequestParam(name = "equityAnnual", required = false) Double equityAnnual,
            @RequestParam(name = "isMarried", required = false) Boolean isMarried,
            @RequestParam(name = "signingBonus", required = false, defaultValue = "0") double signingBonus,
            @RequestParam(name = "equityMultiplier", required = false, defaultValue = "1.0") double equityMultiplier,
            @RequestParam(name = "commuteTime", required = false, defaultValue = "0") double commuteTime,
            @RequestParam(name = "sideHustle", required = false, defaultValue = "0") double sideHustle,
            @RequestParam(name = "otherLeaks", required = false, defaultValue = "0") double otherLeaks,
            @RequestParam(name = "isRemote", required = false, defaultValue = "false") boolean isRemote,
            @RequestParam(name = "isHomeOwner", required = false, defaultValue = "false") boolean isHomeOwner,
            @RequestParam(name = "hasStudentLoan", required = false, defaultValue = "false") boolean hasStudentLoan,
            @RequestParam(name = "isTaxOptimized", required = false, defaultValue = "false") boolean isTaxOptimized,
            @RequestParam(name = "isCarOwner", required = false, defaultValue = "true") boolean isCarOwner,
            RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpServletResponse response,
            Model model) {

        // SEO SIGNAL: Last-Modified Header (Always fresh)
        response.setHeader(org.springframework.http.HttpHeaders.LAST_MODIFIED,
                java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now()));

        String normalizedJob = SlugNormalizer.normalize(job);
        String normalizedCityA = SlugNormalizer.normalize(cityA);
        String normalizedCityB = SlugNormalizer.normalize(cityB);

        JobInfo jobInfo = repository.findJobLoosely(normalizedJob)
                .orElseGet(() -> {
                    // Start of Custom Job Logic
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

        String canonicalPath = "/" + jobInfo.getSlug() + "-salary-" + cityEntryA.getSlug() + "-vs-"
                + cityEntryB.getSlug();

        // Canonical Redirect if slugs are dirty
        if (!job.equals(jobInfo.getSlug()) || !cityA.equals(cityEntryA.getSlug()) || !cityB.equals(cityEntryB.getSlug())
                || !SlugNormalizer.isCanonicalCitySlug(cityEntryA.getSlug())
                || !SlugNormalizer.isCanonicalCitySlug(cityEntryB.getSlug())) {
            RedirectView redirectView = new RedirectView(canonicalPath, true);
            // Pass params if they exist
            if (currentSalary != null && offerSalary != null) {
                addRedirectParams(redirectAttributes, currentSalary, offerSalary, salaryType, fourOhOneKRate,
                        monthlyInsurance, equityAnnual, isMarried, signingBonus, equityMultiplier,
                        commuteTime, sideHustle, otherLeaks, isRemote, isHomeOwner, hasStudentLoan,
                        isTaxOptimized, isCarOwner);
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

        // --- SMART SALARY AUTO-DETECTION & CANONICALIZATION ---

        // 1. Determine Effective Annual Salary
        // Logic:
        // - If explicit "monthly" type: Multiplier = 12
        // - If explicit "annual" type:
        // - If < $12,000: Auto-detect as Monthly (Safety Net) -> Multiplier = 12
        // - If >= $12,000: Trust user (Annual) -> Multiplier = 1

        double currentMultiplier = 1.0;
        double offerMultiplier = 1.0;

        // Logic for Current Salary
        if ("monthly".equalsIgnoreCase(salaryType)) {
            currentMultiplier = 12.0;
        } else {
            // Annual (Default) - Check for Safety Net
            if (currentSalary < 12000) {
                currentMultiplier = 12.0;
            }
        }

        // Logic for Offer Salary
        if ("monthly".equalsIgnoreCase(salaryType)) {
            offerMultiplier = 12.0;
        } else {
            // Annual (Default) - Check for Safety Net
            if (offerSalary < 12000) {
                offerMultiplier = 12.0;
            }
        }

        double effectiveCurrentSalary = currentSalary * currentMultiplier;
        double effectiveOfferSalary = offerSalary * offerMultiplier;

        // SEO Enhancement: Canonicalize salaries to $5K buckets ONLY for the URL
        // generation
        // But do NOT redirect if the user provided specific input.
        // We only redirect if salaries were NULL and we used medians.

        // Ensure non-null for calculation
        HouseholdType parsedHouseholdType = (isMarried != null && isMarried) ? HouseholdType.FAMILY
                : HouseholdType.SINGLE;
        HousingType parsedHousingType = isHomeOwner ? HousingType.OWN : HousingType.RENT;

        Boolean effectiveIsMarried = isMarried != null ? isMarried : (parsedHouseholdType == HouseholdType.FAMILY);

        // Final tax data adjustment
        Double taxFourOhOneK = fourOhOneKRate;
        if (taxFourOhOneK == null) {
            taxFourOhOneK = (isTaxOptimized) ? 0.08 : 0.04;
        }

        double extraDebt = hasStudentLoan ? 800.0 : 0.0;

        String validationMessage = validateSalary(effectiveCurrentSalary).orElse(null);
        validationMessage = validationMessage == null ? validateSalary(effectiveOfferSalary).orElse(null)
                : validationMessage;

        double safeCurrentSalary = clampSalary(effectiveCurrentSalary);
        double safeOfferSalary = clampSalary(effectiveOfferSalary);

        ComparisonResult result = comparisonService.compare(
                cityEntryA.getSlug(),
                cityEntryB.getSlug(),
                safeCurrentSalary,
                safeOfferSalary,
                parsedHouseholdType,
                parsedHousingType,
                effectiveIsMarried,
                taxFourOhOneK,
                monthlyInsurance,
                extraDebt,
                otherLeaks,
                sideHustle,
                isRemote,
                isCarOwner,
                signingBonus,
                equityAnnual != null ? equityAnnual : 0.0,
                equityMultiplier,
                commuteTime);

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

        // Pass EFFECTIVE (Annualized) salaries for display to ensure math adds up
        // visually
        model.addAttribute("currentSalary", effectiveCurrentSalary);
        model.addAttribute("offerSalary", effectiveOfferSalary);

        // Flag for UI: Did we auto-detect monthly input despite "Annual" default?
        boolean autoConverted = !"monthly".equalsIgnoreCase(salaryType)
                && (currentMultiplier > 1.0 || offerMultiplier > 1.0);
        model.addAttribute("autoConverted", autoConverted);

        model.addAttribute("householdType", parsedHouseholdType);
        model.addAttribute("housingType", parsedHousingType);
        model.addAttribute("validationMessage", validationMessage);
        model.addAttribute("cities", repository.getCities());
        model.addAttribute("jobs", repository.getJobs());

        // Build query string using RAW params to preserve user input in links/forms if
        // needed
        // (Or generally, links should probably use the canonical/effective ones?
        // Actually, for related links, maybe stick to what they typed or effective?
        // Let's stick to raw for query string to be safe, or effective?
        // If we use effective, it might clarify future clicks. Let's use effective for
        // links.)
        String queryString = buildQueryString(effectiveCurrentSalary, effectiveOfferSalary, fourOhOneKRate,
                monthlyInsurance, equityAnnual,
                isMarried);
        model.addAttribute("otherJobLinks",
                comparisonService.relatedJobComparisons(cityEntryA.getSlug(), cityEntryB.getSlug(), queryString));
        model.addAttribute("otherCityLinks", relatedCityComparisons(jobInfo.getSlug(),
                cityEntryA.getSlug(), cityEntryB.getSlug(), queryString));

        // Build robust Structured Data (WebPage + FAQ + Breadcrumbs + Dataset)
        Map<String, Object> schemaMap = buildStructuredData(title, metaDescription, canonicalUrl, result, cityEntryA,
                cityEntryB, effectiveOfferSalary, jobInfo);
        model.addAttribute("structuredDataJson", toJson(schemaMap));

        // Pass FAQ list for visible rendering if needed (optional, keeping it in schema
        // for now)
        // model.addAttribute("faqList", schemaMap.get("faqList")); // Future
        // enhancement

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

    /**
     * SEO STRATEGY: SILOING
     * Prioritize cities in the SAME STATE to build topical authority.
     */
    private List<com.offerverdict.model.LinkDTO> relatedCityComparisons(String jobSlug,
            String baseCitySlug,
            String offerCitySlug,
            String queryString) {
        CityCostEntry origin = repository.getCity(baseCitySlug);
        String jobTitle = jobSlug.replace("-", " "); // Simple un-slugify

        return repository.getCities().stream()
                .filter(c -> !c.getSlug().equals(origin.getSlug()))
                .filter(c -> !c.getSlug().equals(offerCitySlug))
                .filter(c -> SlugNormalizer.isCanonicalCitySlug(c.getSlug()))
                // SILO LOGIC: Sort by (Same State?) -> (Tier Priority) -> (Name)
                .sorted(java.util.Comparator
                        .<CityCostEntry, Boolean>comparing(c -> !c.getState().equals(origin.getState())) // True
                                                                                                         // (different)
                                                                                                         // comes last
                        .thenComparingInt(CityCostEntry::getTier)
                        .thenComparing(CityCostEntry::getSlug))
                .limit(6) // Increased to 6 for better internal link density
                .map(c -> {
                    String url = "/" + jobSlug + "-salary-" + origin.getSlug() + "-vs-" + c.getSlug() + queryString;
                    String text = String.format("%s salary in %s vs %s", jobTitle, origin.getCity(), c.getCity());
                    return new com.offerverdict.model.LinkDTO(url, text);
                })
                .collect(Collectors.toList());
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
            return 100000; // Generic
                           // engineer

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
            return 45000; // Including
                          // tips
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
        // PERMANENTLY NOINDEX Custom / User-Generated Jobs to prevent spam
        if ("Custom".equalsIgnoreCase(job.getCategory())) {
            return false;
        }

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
            ComparisonResult result, CityCostEntry cityEntryA, CityCostEntry cityEntryB, double offerSalary,
            JobInfo job) {

        List<Map<String, Object>> graph = new ArrayList<>();

        // 1. WebPage
        Map<String, Object> webpage = new HashMap<>();
        webpage.put("@context", "https://schema.org");
        webpage.put("@type", "WebPage");
        webpage.put("name", title);
        webpage.put("description", description);
        webpage.put("url", canonicalUrl);
        webpage.put("datePublished", "2025-01-01"); // Static start
        webpage.put("dateModified", java.time.LocalDate.now().toString()); // Dynamic freshness
        graph.add(webpage);

        // 2. BreadcrumbList (Hierarchy Power)
        Map<String, Object> breadcrumb = new HashMap<>();
        breadcrumb.put("@context", "https://schema.org");
        breadcrumb.put("@type", "BreadcrumbList");

        List<Map<String, Object>> itemListElement = new ArrayList<>();

        // Home
        itemListElement.add(Map.of(
                "@type", "ListItem",
                "position", 1,
                "name", "Home",
                "item", appProperties.getPublicBaseUrl()));

        // Tools/Salary Check
        itemListElement.add(Map.of(
                "@type", "ListItem",
                "position", 2,
                "name", "Salary Check",
                "item", appProperties.getPublicBaseUrl() + "/cities"));

        // Current Page
        itemListElement.add(Map.of(
                "@type", "ListItem",
                "position", 3,
                "name", String.format("%s in %s vs %s", job.getTitle(), cityEntryA.getCity(), cityEntryB.getCity()),
                "item", canonicalUrl));

        breadcrumb.put("itemListElement", itemListElement);
        graph.add(breadcrumb);

        // 3. Dataset (Authority Signal)
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("@context", "https://schema.org");
        dataset.put("@type", "Dataset");
        dataset.put("name", String.format("%s Salary & Cost of Living Data: %s vs %s", job.getTitle(),
                cityEntryA.getCity(), cityEntryB.getCity()));
        dataset.put("description", String.format(
                "Comprehensive 2026 comparative data for %s stats, including 2025 IRS tax brackets, %s rent prices, and purchasing power parity analysis.",
                job.getTitle(), cityEntryB.getCity()));
        dataset.put("license", "https://creativecommons.org/licenses/by-sa/4.0/");
        dataset.put("creator", Map.of("@type", "Organization", "name", "OfferVerdict"));
        dataset.put("dateModified", java.time.LocalDate.now().toString());
        graph.add(dataset);

        // 4. FAQPage (Dynamic & Specific)
        Map<String, Object> faq = new HashMap<>();
        faq.put("@context", "https://schema.org");
        faq.put("@type", "FAQPage");

        List<Map<String, Object>> questions = new ArrayList<>();

        // Q1: The Verdict
        questions.add(Map.of(
                "@type", "Question",
                "name", String.format("Is a salary of $%s good in %s vs %s?",
                        String.format("%,.0f", offerSalary), cityEntryB.getCity(), cityEntryA.getCity()),
                "acceptedAnswer", Map.of(
                        "@type", "Answer",
                        "text", result.getVerdictCopy() + " " + result.getValueDiffMsg())));

        // Q2: Cost of Living Context (Dynamic Numbers)
        String housingDiff = result.getOffer().getRent() > result.getCurrent().getRent() ? "higher" : "lower";
        double rentDiffVal = Math.abs(result.getOffer().getRent() - result.getCurrent().getRent());

        questions.add(Map.of(
                "@type", "Question",
                "name",
                String.format("How much is rent in %s compared to %s?", cityEntryB.getCity(), cityEntryA.getCity()),
                "acceptedAnswer", Map.of(
                        "@type", "Answer",
                        "text",
                        String.format(
                                "Housing in %s is %s. You would pay roughly $%s/month (~$%s difference) for a similar quality of life.",
                                cityEntryB.getCity(), housingDiff,
                                String.format("%,.0f", result.getOffer().getRent()),
                                String.format("%,.0f", rentDiffVal)))));

        // Q3: Tax Context
        double totalTax = (result.getOffer().getTaxResult() != null)
                ? result.getOffer().getTaxResult().getTotalTax()
                : 0.0;

        questions.add(Map.of(
                "@type", "Question",
                "name",
                String.format("What is the take-home pay for $%s in %s?", String.format("%,.0f", offerSalary),
                        cityEntryB.getCity()),
                "acceptedAnswer", Map.of(
                        "@type", "Answer",
                        "text",
                        String.format(
                                "For a single filer earning $%s in %s, estimated total tax (Federal + State + Local) is roughly $%s/year (Effective Rate: %.1f%%).",
                                String.format("%,.0f", offerSalary), cityEntryB.getCity(),
                                String.format("%,.0f", totalTax),
                                result.getOffer().getTaxResult().getEffectiveTaxRate()))));

        faq.put("mainEntity", questions);
        graph.add(faq);

        Map<String, Object> data = new HashMap<>();
        data.put("@graph", graph);
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
            String salaryType,
            Double fourOhOneKRate,
            Double monthlyInsurance,
            Double equityAnnual,
            Boolean isMarried,
            Double signingBonus,
            Double equityMultiplier,
            Double commuteTime,
            Double sideHustle,
            Double otherLeaks,
            Boolean isRemote,
            Boolean isHomeOwner,
            Boolean hasStudentLoan,
            Boolean isTaxOptimized,
            Boolean isCarOwner) {
        redirectAttributes.addAttribute("currentSalary", currentSalary);
        redirectAttributes.addAttribute("offerSalary", offerSalary);
        if (salaryType != null && !salaryType.equals("annual"))
            redirectAttributes.addAttribute("salaryType", salaryType);
        if (fourOhOneKRate != null)
            redirectAttributes.addAttribute("fourOhOneKRate", fourOhOneKRate);
        if (monthlyInsurance != null)
            redirectAttributes.addAttribute("monthlyInsurance", monthlyInsurance);
        if (equityAnnual != null)
            redirectAttributes.addAttribute("equityAnnual", equityAnnual);
        if (isMarried != null)
            redirectAttributes.addAttribute("isMarried", isMarried);
        if (signingBonus != null)
            redirectAttributes.addAttribute("signingBonus", signingBonus);
        if (equityMultiplier != null)
            redirectAttributes.addAttribute("equityMultiplier", equityMultiplier);
        if (commuteTime != null)
            redirectAttributes.addAttribute("commuteTime", commuteTime);
        if (sideHustle != null)
            redirectAttributes.addAttribute("sideHustle", sideHustle);
        if (otherLeaks != null)
            redirectAttributes.addAttribute("otherLeaks", otherLeaks);
        if (isRemote != null)
            redirectAttributes.addAttribute("isRemote", isRemote);
        if (isHomeOwner != null)
            redirectAttributes.addAttribute("isHomeOwner", isHomeOwner);
        if (hasStudentLoan != null)
            redirectAttributes.addAttribute("hasStudentLoan", hasStudentLoan);
        if (isTaxOptimized != null)
            redirectAttributes.addAttribute("isTaxOptimized", isTaxOptimized);
        if (isCarOwner != null)
            redirectAttributes.addAttribute("isCarOwner", isCarOwner);
    }

    private String buildQueryString(double currentSalary, double offerSalary,
            Double fourOhOneKRate, Double monthlyInsurance,
            Double equityAnnual, Boolean isMarried) {
        StringBuilder sb = new StringBuilder();
        sb.append("?currentSalary=").append(currentSalary);
        sb.append("&offerSalary=").append(offerSalary);
        if (fourOhOneKRate != null)
            sb.append("&fourOhOneKRate=").append(fourOhOneKRate);
        if (monthlyInsurance != null)
            sb.append("&monthlyInsurance=").append(monthlyInsurance);
        if (equityAnnual != null)
            sb.append("&equityAnnual=").append(equityAnnual);
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