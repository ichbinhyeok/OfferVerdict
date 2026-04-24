package com.offerverdict.controller;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import com.offerverdict.model.OfferDocumentExtractResult;
import com.offerverdict.model.OfferRiskDraft;
import com.offerverdict.model.OfferRiskReport;
import com.offerverdict.model.OfferTextParseResult;
import com.offerverdict.service.ComparisonService;
import com.offerverdict.service.OfferDocumentExtractService;
import com.offerverdict.service.OfferRiskService;
import com.offerverdict.service.OfferTextParserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;

@Controller
public class OfferRiskController {
    private static final List<String> HEALTHCARE_ROLE_SLUGS = List.of(
            "registered-nurse",
            "physical-therapist",
            "pharmacist",
            "medical-resident");

    private final DataRepository repository;
    private final OfferRiskService offerRiskService;
    private final ComparisonService comparisonService;
    private final OfferTextParserService offerTextParserService;
    private final OfferDocumentExtractService offerDocumentExtractService;

    public OfferRiskController(DataRepository repository,
            OfferRiskService offerRiskService,
            ComparisonService comparisonService,
            OfferTextParserService offerTextParserService,
            OfferDocumentExtractService offerDocumentExtractService) {
        this.repository = repository;
        this.offerRiskService = offerRiskService;
        this.comparisonService = comparisonService;
        this.offerTextParserService = offerTextParserService;
        this.offerDocumentExtractService = offerDocumentExtractService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(name = "mode", defaultValue = "offer_review") String mode,
            @RequestParam(name = "issue", defaultValue = "") String issue,
            Model model) {
        renderTool(model, variantFor("/"), OfferRiskDraft.manualDefaults(mode), null, issueContext(issue));
        return "offer-risk-tool";
    }

    @GetMapping("/nurse-relocation-offer-checker")
    public String nurseRelocationOfferChecker(
            @RequestParam(name = "mode", defaultValue = "offer_review") String mode,
            @RequestParam(name = "issue", defaultValue = "") String issue,
            Model model) {
        renderTool(model, variantFor("/nurse-relocation-offer-checker"), OfferRiskDraft.manualDefaults(mode), null,
                issueContext(issue));
        return "offer-risk-tool";
    }

    @GetMapping("/sign-on-bonus-repayment-calculator")
    public String signOnBonusCalculator(
            @RequestParam(name = "mode", defaultValue = "offer_review") String mode,
            @RequestParam(name = "issue", defaultValue = "") String issue,
            Model model) {
        renderTool(model, variantFor("/sign-on-bonus-repayment-calculator"), OfferRiskDraft.manualDefaults(mode), null,
                issueContext(issue));
        return "offer-risk-tool";
    }

    @GetMapping("/shift-differential-calculator")
    public String shiftDifferentialCalculator(
            @RequestParam(name = "mode", defaultValue = "offer_review") String mode,
            @RequestParam(name = "issue", defaultValue = "") String issue,
            Model model) {
        renderTool(model, variantFor("/shift-differential-calculator"), OfferRiskDraft.manualDefaults(mode), null,
                issueContext(issue));
        return "offer-risk-tool";
    }

    @PostMapping("/offer-risk-draft")
    public String parseOfferDraft(
            @RequestParam(name = "variant", defaultValue = "/nurse-relocation-offer-checker") String variant,
            @RequestParam(name = "analysisMode", defaultValue = "offer_review") String analysisMode,
            @RequestParam(name = "issue", defaultValue = "") String issue,
            @RequestParam(name = "sourceText", defaultValue = "") String sourceText,
            @RequestParam(name = "sourceFile", required = false) MultipartFile sourceFile,
            Model model) {
        OfferDocumentExtractResult extractedDocument = offerDocumentExtractService.extract(sourceText, sourceFile);
        OfferTextParseResult parseResult = offerTextParserService.parse(extractedDocument.getSourceText(), analysisMode);
        parseResult.setSourceLabel(extractedDocument.getSourceLabel());
        parseResult.setParseWarning(mergeWarnings(parseResult.getParseWarning(), extractedDocument.getWarning()));
        if (extractedDocument.isFromFile() && parseResult.getSummary() != null) {
            parseResult.setSummary(parseResult.getSummary().replace("pasted text", "uploaded document"));
        }
        renderTool(model, variantFor(variant), parseResult.getDraft(), parseResult, issueContext(issue));
        return "offer-risk-tool";
    }

    @GetMapping("/offer-risk-report")
    public String report(
            @RequestParam(name = "analysisMode", defaultValue = "offer_review") String analysisMode,
            @RequestParam(name = "roleSlug", defaultValue = "registered-nurse") String roleSlug,
            @RequestParam(name = "currentCitySlug", defaultValue = "austin-tx") String currentCitySlug,
            @RequestParam(name = "offerCitySlug", defaultValue = "seattle-wa") String offerCitySlug,
            @RequestParam(name = "unitType", defaultValue = "med_surg") String unitType,
            @RequestParam(name = "shiftGuarantee", defaultValue = "written") String shiftGuarantee,
            @RequestParam(name = "floatRisk", defaultValue = "home_unit_only") String floatRisk,
            @RequestParam(name = "cancelRisk", defaultValue = "protected_hours") String cancelRisk,
            @RequestParam(name = "currentHourlyRate", defaultValue = "42") double currentHourlyRate,
            @RequestParam(name = "offerHourlyRate", defaultValue = "56") double offerHourlyRate,
            @RequestParam(name = "weeklyHours", defaultValue = "36") double weeklyHours,
            @RequestParam(name = "overtimeHours", defaultValue = "0") double overtimeHours,
            @RequestParam(name = "nightDiffPercent", defaultValue = "12") double nightDiffPercent,
            @RequestParam(name = "nightHours", defaultValue = "0") double nightHours,
            @RequestParam(name = "weekendDiffPercent", defaultValue = "8") double weekendDiffPercent,
            @RequestParam(name = "weekendHours", defaultValue = "0") double weekendHours,
            @RequestParam(name = "currentMonthlyInsurance", defaultValue = "150") double currentMonthlyInsurance,
            @RequestParam(name = "offerMonthlyInsurance", defaultValue = "150") double offerMonthlyInsurance,
            @RequestParam(name = "signOnBonus", defaultValue = "15000") double signOnBonus,
            @RequestParam(name = "relocationStipend", defaultValue = "5000") double relocationStipend,
            @RequestParam(name = "movingCost", defaultValue = "7000") double movingCost,
            @RequestParam(name = "contractMonths", defaultValue = "24") int contractMonths,
            @RequestParam(name = "plannedStayMonths", defaultValue = "12") int plannedStayMonths,
            @RequestParam(name = "repaymentStyle", defaultValue = "prorated") String repaymentStyle,
            @RequestParam(name = "issue", defaultValue = "") String issue,
            Model model) {
        OfferRiskDraft draft = reportDraft(analysisMode, roleSlug, currentCitySlug, offerCitySlug, unitType,
                shiftGuarantee, floatRisk, cancelRisk, currentHourlyRate, offerHourlyRate, weeklyHours,
                overtimeHours, nightDiffPercent, nightHours, weekendDiffPercent, weekendHours,
                currentMonthlyInsurance, offerMonthlyInsurance, signOnBonus, relocationStipend, movingCost,
                contractMonths, plannedStayMonths, repaymentStyle, "");
        renderReport(model, draft, "", "", issueContext(issue));
        return "offer-risk-report";
    }

    @PostMapping("/offer-risk-report")
    public String reportFromDocument(
            @RequestParam(name = "analysisMode", defaultValue = "offer_review") String analysisMode,
            @RequestParam(name = "roleSlug", defaultValue = "registered-nurse") String roleSlug,
            @RequestParam(name = "currentCitySlug", defaultValue = "austin-tx") String currentCitySlug,
            @RequestParam(name = "offerCitySlug", defaultValue = "seattle-wa") String offerCitySlug,
            @RequestParam(name = "unitType", defaultValue = "med_surg") String unitType,
            @RequestParam(name = "shiftGuarantee", defaultValue = "written") String shiftGuarantee,
            @RequestParam(name = "floatRisk", defaultValue = "home_unit_only") String floatRisk,
            @RequestParam(name = "cancelRisk", defaultValue = "protected_hours") String cancelRisk,
            @RequestParam(name = "currentHourlyRate", defaultValue = "42") double currentHourlyRate,
            @RequestParam(name = "offerHourlyRate", defaultValue = "56") double offerHourlyRate,
            @RequestParam(name = "weeklyHours", defaultValue = "36") double weeklyHours,
            @RequestParam(name = "overtimeHours", defaultValue = "0") double overtimeHours,
            @RequestParam(name = "nightDiffPercent", defaultValue = "12") double nightDiffPercent,
            @RequestParam(name = "nightHours", defaultValue = "0") double nightHours,
            @RequestParam(name = "weekendDiffPercent", defaultValue = "8") double weekendDiffPercent,
            @RequestParam(name = "weekendHours", defaultValue = "0") double weekendHours,
            @RequestParam(name = "currentMonthlyInsurance", defaultValue = "150") double currentMonthlyInsurance,
            @RequestParam(name = "offerMonthlyInsurance", defaultValue = "150") double offerMonthlyInsurance,
            @RequestParam(name = "signOnBonus", defaultValue = "15000") double signOnBonus,
            @RequestParam(name = "relocationStipend", defaultValue = "5000") double relocationStipend,
            @RequestParam(name = "movingCost", defaultValue = "7000") double movingCost,
            @RequestParam(name = "contractMonths", defaultValue = "24") int contractMonths,
            @RequestParam(name = "plannedStayMonths", defaultValue = "12") int plannedStayMonths,
            @RequestParam(name = "repaymentStyle", defaultValue = "prorated") String repaymentStyle,
            @RequestParam(name = "sourceText", defaultValue = "") String sourceText,
            @RequestParam(name = "documentSourceLabel", defaultValue = "") String documentSourceLabel,
            @RequestParam(name = "documentParseWarning", defaultValue = "") String documentParseWarning,
            @RequestParam(name = "issue", defaultValue = "") String issue,
            Model model) {
        OfferRiskDraft draft = reportDraft(analysisMode, roleSlug, currentCitySlug, offerCitySlug, unitType,
                shiftGuarantee, floatRisk, cancelRisk, currentHourlyRate, offerHourlyRate, weeklyHours,
                overtimeHours, nightDiffPercent, nightHours, weekendDiffPercent, weekendHours,
                currentMonthlyInsurance, offerMonthlyInsurance, signOnBonus, relocationStipend, movingCost,
                contractMonths, plannedStayMonths, repaymentStyle, sourceText);
        renderReport(model, draft, documentSourceLabel, documentParseWarning, issueContext(issue));
        return "offer-risk-report";
    }

    private void renderReport(Model model,
            OfferRiskDraft draft,
            String documentSourceLabel,
            String documentParseWarning,
            NurseOfferIssueController.IssueContext issueContext) {
        OfferRiskReport report = offerRiskService.assess(draft.getAnalysisMode(), draft.getRoleSlug(),
                draft.getCurrentCitySlug(), draft.getOfferCitySlug(), draft.getUnitType(), draft.getShiftGuarantee(),
                draft.getFloatRisk(), draft.getCancelRisk(), draft.getCurrentHourlyRate(), draft.getOfferHourlyRate(),
                draft.getWeeklyHours(), draft.getOvertimeHours(), draft.getNightDiffPercent(), draft.getNightHours(),
                draft.getWeekendDiffPercent(), draft.getWeekendHours(), draft.getCurrentMonthlyInsurance(),
                draft.getOfferMonthlyInsurance(), draft.getSignOnBonus(), draft.getRelocationStipend(),
                draft.getMovingCost(), draft.getContractMonths(), draft.getPlannedStayMonths(),
                draft.getRepaymentStyle(), draft.getSourceText());

        model.addAttribute("report", report);
        model.addAttribute("title",
                report.getAnalysisModeLabel() + " | " + report.getVerdict() + " | " + report.getRoleLabel());
        model.addAttribute("metaDescription",
                report.getAnalysisModeLabel()
                        + " with local cash, schedule risk, incentives, and next-step questions for the listing or offer.");
        model.addAttribute("canonicalUrl", comparisonService.buildCanonicalUrl("/offer-risk-report"));
        model.addAttribute("shouldIndex", false);
        model.addAttribute("cities", sortedCities());
        model.addAttribute("roles", healthcareRoles());
        addIssueContext(model, issueContext);
        OfferTextParseResult documentParse = buildDocumentParse(draft.getSourceText(), draft.getAnalysisMode(),
                documentSourceLabel, documentParseWarning);
        if (documentParse != null) {
            model.addAttribute("documentParse", documentParse);
        }
    }

    private OfferRiskDraft reportDraft(String analysisMode,
            String roleSlug,
            String currentCitySlug,
            String offerCitySlug,
            String unitType,
            String shiftGuarantee,
            String floatRisk,
            String cancelRisk,
            double currentHourlyRate,
            double offerHourlyRate,
            double weeklyHours,
            double overtimeHours,
            double nightDiffPercent,
            double nightHours,
            double weekendDiffPercent,
            double weekendHours,
            double currentMonthlyInsurance,
            double offerMonthlyInsurance,
            double signOnBonus,
            double relocationStipend,
            double movingCost,
            int contractMonths,
            int plannedStayMonths,
            String repaymentStyle,
            String sourceText) {
        OfferRiskDraft draft = OfferRiskDraft.manualDefaults(analysisMode);
        draft.setRoleSlug(roleSlug);
        draft.setCurrentCitySlug(currentCitySlug);
        draft.setOfferCitySlug(offerCitySlug);
        draft.setUnitType(unitType);
        draft.setShiftGuarantee(shiftGuarantee);
        draft.setFloatRisk(floatRisk);
        draft.setCancelRisk(cancelRisk);
        draft.setCurrentHourlyRate(currentHourlyRate);
        draft.setOfferHourlyRate(offerHourlyRate);
        draft.setWeeklyHours(weeklyHours);
        draft.setOvertimeHours(overtimeHours);
        draft.setNightDiffPercent(nightDiffPercent);
        draft.setNightHours(nightHours);
        draft.setWeekendDiffPercent(weekendDiffPercent);
        draft.setWeekendHours(weekendHours);
        draft.setCurrentMonthlyInsurance(currentMonthlyInsurance);
        draft.setOfferMonthlyInsurance(offerMonthlyInsurance);
        draft.setSignOnBonus(signOnBonus);
        draft.setRelocationStipend(relocationStipend);
        draft.setMovingCost(movingCost);
        draft.setContractMonths(contractMonths);
        draft.setPlannedStayMonths(plannedStayMonths);
        draft.setRepaymentStyle(repaymentStyle);
        draft.setSourceText(sourceText);
        return draft;
    }

    private OfferTextParseResult buildDocumentParse(String sourceText,
            String analysisMode,
            String documentSourceLabel,
            String documentParseWarning) {
        if (sourceText == null || sourceText.isBlank()) {
            return null;
        }
        OfferTextParseResult documentParse = offerTextParserService.parse(sourceText, analysisMode);
        documentParse.setSourceLabel(documentSourceLabel == null || documentSourceLabel.isBlank()
                ? defaultSourceLabel(analysisMode)
                : documentSourceLabel);
        if (documentParseWarning != null && !documentParseWarning.isBlank()) {
            documentParse.setParseWarning(documentParseWarning);
        }
        return documentParse;
    }

    private String mergeWarnings(String primary, String secondary) {
        if (primary == null || primary.isBlank()) {
            return secondary;
        }
        if (secondary == null || secondary.isBlank()) {
            return primary;
        }
        if (primary.equalsIgnoreCase(secondary)) {
            return primary;
        }
        return primary + " " + secondary;
    }

    private String defaultSourceLabel(String analysisMode) {
        return "job_post".equals(analysisMode) ? "Pasted listing text" : "Pasted offer text";
    }

    private void renderTool(Model model,
            ToolVariant variant,
            OfferRiskDraft draft,
            OfferTextParseResult parseResult,
            NurseOfferIssueController.IssueContext issueContext) {
        String activeMode = draft.getAnalysisMode();
        OfferRiskDraft offerDraft = "job_post".equals(activeMode)
                ? OfferRiskDraft.manualDefaults("offer_review")
                : draft;
        OfferRiskDraft jobPostDraft = "job_post".equals(activeMode)
                ? draft
                : OfferRiskDraft.manualDefaults("job_post");

        model.addAttribute("title", variant.title());
        model.addAttribute("metaDescription", variant.metaDescription());
        model.addAttribute("canonicalUrl", comparisonService.buildCanonicalUrl(variant.canonicalPath()));
        model.addAttribute("shouldIndex", true);
        model.addAttribute("cities", sortedCities());
        model.addAttribute("roles", healthcareRoles());
        model.addAttribute("pageHeading", variant.pageHeading());
        model.addAttribute("pageLead", variant.pageLead());
        model.addAttribute("variantPath", variant.canonicalPath());
        model.addAttribute("activeMode", activeMode);
        model.addAttribute("offerDraft", offerDraft);
        model.addAttribute("jobPostDraft", jobPostDraft);
        model.addAttribute("parseResult", parseResult);
        addIssueContext(model, issueContext);
    }

    private NurseOfferIssueController.IssueContext issueContext(String issue) {
        return NurseOfferIssueController.issueContextFor(issue);
    }

    private void addIssueContext(Model model, NurseOfferIssueController.IssueContext issueContext) {
        model.addAttribute("issueContext", issueContext);
        model.addAttribute("issueSlug", issueContext == null ? "" : issueContext.slug());
    }

    private List<CityCostEntry> sortedCities() {
        return repository.getCities().stream()
                .sorted(Comparator.comparingInt(CityCostEntry::getPriority)
                        .thenComparing(CityCostEntry::getCity))
                .toList();
    }

    private List<JobInfo> healthcareRoles() {
        return HEALTHCARE_ROLE_SLUGS.stream()
                .map(repository::findJobLoosely)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private ToolVariant variantFor(String canonicalPath) {
        if ("/shift-differential-calculator".equals(canonicalPath)) {
            return new ToolVariant(
                    "RN Offer Letter Review | Shift Differential Risk",
                    "Paste an RN offer letter to see when night, weekend, and overtime upside is hiding a weaker base rate or a riskier unit than it looks on paper.",
                    "/shift-differential-calculator",
                    "Paste your RN offer letter before you sign.",
                    "Catch the clause, unit risk, or weak guarantee that can make differential-heavy upside feel safer than it really is.");
        }
        if ("/sign-on-bonus-repayment-calculator".equals(canonicalPath)) {
            return new ToolVariant(
                    "RN Offer Letter Review | Bonus Repayment Risk",
                    "Paste an RN offer letter to see whether sign-on or relocation money creates a real RN clawback trap before you commit.",
                    "/sign-on-bonus-repayment-calculator",
                    "Paste your RN offer letter before you sign.",
                    "Catch the bonus, relocation, or repayment language that can turn a good-looking offer into a bad decision.");
        }
        if ("/".equals(canonicalPath) || "/nurse-relocation-offer-checker".equals(canonicalPath)) {
            return new ToolVariant(
                    "Should I Sign This Nurse Offer? | RN OfferVerdict",
                    "Paste an RN offer letter, recruiter package, or plain-English hesitation to judge pay, clawbacks, schedule, family fit, unit culture, and survivability before you sign.",
                    canonicalPath,
                    "Paste the RN offer that is making you hesitate.",
                    "Get a sign, negotiate, or walk-away read that includes money, contract downside, unit survivability, and the real-life constraint behind your hesitation.");
        }
        return variantFor("/nurse-relocation-offer-checker");
    }

    private record ToolVariant(String title,
                               String metaDescription,
                               String canonicalPath,
                               String pageHeading,
                               String pageLead) {
    }
}
