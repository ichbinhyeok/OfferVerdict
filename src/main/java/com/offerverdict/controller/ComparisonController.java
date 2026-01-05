package com.offerverdict.controller;

import com.offerverdict.config.AppProperties;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonResult;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import com.offerverdict.model.JobInfo;
import com.offerverdict.model.Verdict;
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
    public String home(@RequestParam(required = false) String job,
                       @RequestParam(required = false) String cityA,
                       @RequestParam(required = false) String cityB,
                       @RequestParam(required = false) Double currentSalary,
                       @RequestParam(required = false) Double offerSalary,
                       @RequestParam(required = false, defaultValue = "SINGLE") String householdType,
                       @RequestParam(required = false, defaultValue = "RENT") String housingType,
                       Model model) {

        // 1. 모든 필수 파라미터가 있으면 결과 페이지로 리다이렉트 (pSEO 연결)
        if (job != null && cityA != null && cityB != null && currentSalary != null && offerSalary != null) {
            String normalizedJob = SlugNormalizer.normalize(job);
            String normalizedCityA = SlugNormalizer.normalize(cityA);
            String normalizedCityB = SlugNormalizer.normalize(cityB);

            if (repository.hasJob(normalizedJob) && repository.hasCity(normalizedCityA) && repository.hasCity(normalizedCityB)) {
                // [수정 포인트] RedirectView 객체 대신 문자열 "redirect:..."를 리턴합니다.
                // .name()을 사용하여 Enum 값을 문자열 파라미터로 안전하게 전달합니다.
                return String.format("redirect:/%s-salary-%s-vs-%s?currentSalary=%.0f&offerSalary=%.0f&householdType=%s&housingType=%s",
                        normalizedJob,
                        normalizedCityA,
                        normalizedCityB,
                        currentSalary,
                        offerSalary,
                        normalizeHouseholdType(householdType).name(),
                        normalizeHousingType(housingType).name());
            }
        }

        // 2. 파라미터가 없거나 첫 진입 시: 홈 페이지(index.html) 렌더링
        // 이 데이터들이 주입되어야 드롭다운이 정상적으로 작동합니다.
        model.addAttribute("jobs", repository.getJobs());
        model.addAttribute("cities", repository.getCities());
        model.addAttribute("title", "OfferVerdict | Reality-check your job move");
        model.addAttribute("metaDescription", "Compare your actual buying power with taxes and cost of living.");

        return "index"; // templates/index.html 파일명을 문자열로 반환
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

        JobInfo jobInfo = repository.getJob(normalizedJob);
        CityCostEntry cityEntryA = repository.getCity(normalizedCityA);
        CityCostEntry cityEntryB = repository.getCity(normalizedCityB);

        String canonicalPath = "/" + jobInfo.getSlug() + "-salary-" + cityEntryA.getSlug() + "-vs-" + cityEntryB.getSlug();
        if (!job.equals(jobInfo.getSlug()) || !cityA.equals(cityEntryA.getSlug()) || !cityB.equals(cityEntryB.getSlug())
                || !SlugNormalizer.isCanonicalCitySlug(cityEntryA.getSlug()) || !SlugNormalizer.isCanonicalCitySlug(cityEntryB.getSlug())) {
            RedirectView redirectView = new RedirectView(canonicalPath, true);
            redirectAttributes.addAttribute("currentSalary", currentSalary);
            redirectAttributes.addAttribute("offerSalary", offerSalary);
            redirectAttributes.addAttribute("householdType", normalizeHouseholdType(householdType).name());
            redirectAttributes.addAttribute("housingType", normalizeHousingType(housingType).name());
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
            return redirectView;
        }

        HouseholdType parsedHouseholdType = normalizeHouseholdType(householdType);
        HousingType parsedHousingType = normalizeHousingType(housingType);

        String validationMessage = validateSalary(currentSalary).orElse(null);
        validationMessage = validationMessage == null ? validateSalary(offerSalary).orElse(null) : validationMessage;

        double safeCurrentSalary = clampSalary(currentSalary);
        double safeOfferSalary = clampSalary(offerSalary);

        ComparisonResult result = comparisonService.compare(cityEntryA.getSlug(), cityEntryB.getSlug(), safeCurrentSalary, safeOfferSalary, parsedHouseholdType, parsedHousingType);

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
        model.addAttribute("result", result);
        model.addAttribute("currentSalary", currentSalary);
        model.addAttribute("offerSalary", offerSalary);
        model.addAttribute("householdType", parsedHouseholdType);
        model.addAttribute("housingType", parsedHousingType);
        model.addAttribute("validationMessage", validationMessage);
        model.addAttribute("cities", repository.getCities());
        model.addAttribute("jobs", repository.getJobs());
        model.addAttribute("otherJobLinks", comparisonService.relatedJobComparisons(cityEntryA.getSlug(), cityEntryB.getSlug(), buildQueryString(currentSalary, offerSalary, parsedHouseholdType, parsedHousingType)));
        model.addAttribute("otherCityLinks", comparisonService.relatedCityComparisons(jobInfo.getSlug(), cityEntryA.getSlug(), cityEntryB.getSlug(), buildQueryString(currentSalary, offerSalary, parsedHouseholdType, parsedHousingType)));
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

    private HouseholdType normalizeHouseholdType(String householdType) {
        try {
            return HouseholdType.valueOf(householdType.toUpperCase());
        } catch (Exception e) {
            return HouseholdType.SINGLE;
        }
    }

    private HousingType normalizeHousingType(String housingType) {
        try {
            return HousingType.valueOf(housingType.toUpperCase());
        } catch (Exception e) {
            return HousingType.RENT;
        }
    }

    private String buildQueryString(double currentSalary, double offerSalary, HouseholdType householdType, HousingType housingType) {
        return "?currentSalary=" + currentSalary
                + "&offerSalary=" + offerSalary
                + "&householdType=" + householdType.name()
                + "&housingType=" + housingType.name();
    }

    @GetMapping("/admin/reload-data")
    public ResponseEntity<String> reloadData() {
        if (!appProperties.isDevReloadEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Reload disabled");
        }
        repository.reload();
        return ResponseEntity.ok("Data reloaded");
    }
}
