package com.offerverdict.service;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.OfferRiskDraft;
import com.offerverdict.model.OfferTextParseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OfferTextParserService {
    private static final String HOURLY_NUMBER_CAPTURE = "(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?)";
    private static final Pattern CURRENT_CONTEXT = Pattern.compile(
            "\\b(current|currently|existing|today|now|my current|i make|i earn|i am|i'm|i’m|making|from|live in|work in|living in|working in|based in)\\b");
    private static final Pattern OFFER_CONTEXT = Pattern.compile(
            "\\b(offer|offered|new role|new job|position|job posting|opportunity|relocat|to|located in|assignment|base rate)\\b");
    private static final Pattern HOURLY_RATE_PATTERN = Pattern.compile(
            "\\$?\\s*" + HOURLY_NUMBER_CAPTURE + "\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LABELED_HOURLY_RATE_PATTERN = Pattern.compile(
            "\\b(?:current\\s+(?:pay|rate)|offer\\s+(?:pay|rate)|rate|base\\s+rate|compensation)\\s*[:=]?\\s*\\$?\\s*"
                    + HOURLY_NUMBER_CAPTURE
                    + "\\b(?!(?:\\.\\d)|\\d|\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURLY_RANGE_PATTERN = Pattern.compile(
            "\\$?\\s*" + HOURLY_NUMBER_CAPTURE
                    + "\\s*(?:-|to|–|—)\\s*\\$?\\s*" + HOURLY_NUMBER_CAPTURE
                    + "\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PAY_RANGE_MIN_MAX_PATTERN = Pattern.compile(
            "(?:pay\\s*)?(?:ra)?nge\\s*minimum\\s*:?\\s*\\$?\\s*" + HOURLY_NUMBER_CAPTURE
                    + "(?:\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly))?"
                    + "[\\s\\S]{0,80}?(?:pay\\s*)?(?:ra)?nge\\s*maximum\\s*:?\\s*\\$?\\s*" + HOURLY_NUMBER_CAPTURE
                    + "(?:\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PAY_RANGE_LOW_PATTERN = Pattern.compile(
            "(?:pay\\s*)?(?:ra)?nge[^\\n\\r]{0,20}?m\\w{3,8}\\s*:?\\s*\\$?\\s*" + HOURLY_NUMBER_CAPTURE
                    + "(?:\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PAY_RANGE_HIGH_PATTERN = Pattern.compile(
            "(?:pay\\s*)?(?:ra)?nge[^\\n\\r]{0,20}?max\\w*\\s*:?\\s*\\$?\\s*" + HOURLY_NUMBER_CAPTURE
                    + "(?:\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY_TOKEN_PATTERN = Pattern.compile(
            "\\$\\s*(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?(?:\\s*[kK])?|\\b\\d+(?:\\.\\d{1,2})?\\s*[kK]\\b");
    private static final String MONEY_CAPTURE =
            "(\\$\\s*(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?(?:\\s*[kK])?|\\b\\d+(?:\\.\\d{1,2})?\\s*[kK]\\b)";
    private static final Pattern HOURLY_SUFFIX_PATTERN = Pattern.compile(
            "^\\s*(?:/\\s*(?:hr|hrs|hour)|per\\s*hour|hourly)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTHS_PATTERN = Pattern.compile("\\b(\\d{1,2})\\s*-?\\s*month(?:s)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS_PATTERN = Pattern.compile(
            "\\b(\\d{2})\\s*(?:hours|hrs?|h)\\s*(?:per\\s*week|/\\s*(?:week|wk)|weekly|wk)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern THREE_TWELVES_PATTERN = Pattern.compile("\\b(?:3\\s*x\\s*12|three\\s*12)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENT_FTE_PATTERN = Pattern.compile("\\b(\\d{1,3})\\s*%\\s*fte\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FTE_PATTERN = Pattern.compile("\\b(0\\.\\d{1,2}|1\\.0)\\s*fte\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NIGHT_PERCENT_PATTERN = Pattern.compile(
            "\\bnight(?:\\s*shift)?\\s*(?:differential|diff|premium)[^\\n\\r%$]{0,40}?(\\d{1,2}(?:\\.\\d{1,2})?)\\s*%",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WEEKEND_PERCENT_PATTERN = Pattern.compile(
            "\\bweekend(?:\\s*shift)?\\s*(?:differential|diff|premium)[^\\n\\r%$]{0,40}?(\\d{1,2}(?:\\.\\d{1,2})?)\\s*%",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NIGHT_DOLLAR_PATTERN = Pattern.compile(
            "\\bnight(?:\\s*shift)?\\s*(?:differential|diff|premium)[^\\n\\r$]{0,40}?\\$\\s*(\\d{1,2}(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WEEKEND_DOLLAR_PATTERN = Pattern.compile(
            "\\bweekend(?:\\s*shift)?\\s*(?:differential|diff|premium)[^\\n\\r$]{0,40}?\\$\\s*(\\d{1,2}(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE);

    private final DataRepository repository;

    public OfferTextParserService(DataRepository repository) {
        this.repository = repository;
    }

    public OfferTextParseResult parse(String sourceText, String analysisMode) {
        OfferRiskDraft draft = OfferRiskDraft.parsedDefaults(sourceText, analysisMode);
        boolean jobPostMode = "job_post".equals(draft.getAnalysisMode());
        OfferTextParseResult result = new OfferTextParseResult();
        result.setAnalysisMode(draft.getAnalysisMode());
        result.setDraft(draft);

        String text = normalize(sourceText);
        if (text.isBlank()) {
            result.setParsed(false);
            result.setSummary(jobPostMode
                    ? "Paste a job post or recruiter note to auto-fill the quick screen."
                    : "Paste an offer letter or recruiter note to auto-fill the review.");
            return result;
        }

        String lowerText = text.toLowerCase(Locale.US);
        Set<String> extracted = new LinkedHashSet<>();
        List<String> missing = new ArrayList<>();

        boolean foundCurrentCity = false;
        boolean foundOfferCity = false;
        boolean foundCurrentRate = false;
        boolean foundOfferRate = false;
        boolean foundPlannedStay = false;
        boolean foundMovingCost = false;

        Optional<String> roleSlug = detectRoleSlug(lowerText);
        if (roleSlug.isPresent()) {
            draft.setRoleSlug(roleSlug.get());
            extracted.add("Role: " + readableRole(roleSlug.get()));
        }

        Optional<String> unitType = detectUnitType(lowerText);
        if (unitType.isPresent()) {
            draft.setUnitType(unitType.get());
            extracted.add("Unit: " + readableUnit(unitType.get()));
        }

        Optional<String> shiftGuarantee = detectShiftGuarantee(lowerText);
        if (shiftGuarantee.isPresent()) {
            draft.setShiftGuarantee(shiftGuarantee.get());
            extracted.add("Shift terms: " + readableShiftGuarantee(shiftGuarantee.get()));
        }

        Optional<String> floatRisk = detectFloatRisk(lowerText, draft.getUnitType());
        if (floatRisk.isPresent()) {
            draft.setFloatRisk(floatRisk.get());
            extracted.add("Float terms: " + readableFloatRisk(floatRisk.get()));
        }

        Optional<String> cancelRisk = detectCancelRisk(lowerText);
        if (cancelRisk.isPresent()) {
            draft.setCancelRisk(cancelRisk.get());
            extracted.add("Cancellation terms: " + readableCancelRisk(cancelRisk.get()));
        }

        CityAssignment cityAssignment = detectCities(lowerText);
        if (cityAssignment.currentCitySlug() != null) {
            draft.setCurrentCitySlug(cityAssignment.currentCitySlug());
            extracted.add("Current city: " + cityAssignment.currentCityLabel());
            foundCurrentCity = true;
        }
        if (cityAssignment.offerCitySlug() != null) {
            draft.setOfferCitySlug(cityAssignment.offerCitySlug());
            extracted.add("Offer city: " + cityAssignment.offerCityLabel());
            foundOfferCity = true;
        }

        HourlyAssignment hourlyAssignment = detectHourlyRates(text, lowerText, jobPostMode);
        if (hourlyAssignment.currentRate() != null) {
            draft.setCurrentHourlyRate(hourlyAssignment.currentRate());
            extracted.add("Current hourly rate: $" + dollars(hourlyAssignment.currentRate()) + "/hr");
            foundCurrentRate = true;
        }
        if (hourlyAssignment.offerRate() != null) {
            draft.setOfferHourlyRate(hourlyAssignment.offerRate());
            if (hourlyAssignment.rangeDetected()) {
                extracted.add(hourlyAssignment.selectionLabel());
                result.setParseWarning(mergeWarnings(result.getParseWarning(), hourlyAssignment.selectionWarning()));
            } else {
                extracted.add("Offer hourly rate: $" + dollars(hourlyAssignment.offerRate()) + "/hr");
            }
            foundOfferRate = true;
        }

        Double weeklyHours = detectWeeklyHours(text);
        if (weeklyHours != null) {
            draft.setWeeklyHours(weeklyHours);
            extracted.add("Scheduled hours: " + dollars(weeklyHours) + "/week");
        }

        Double nightDiffPercent = detectPercentOrDollarDiff(text, draft.getOfferHourlyRate(), NIGHT_PERCENT_PATTERN,
                NIGHT_DOLLAR_PATTERN);
        if (nightDiffPercent != null) {
            draft.setNightDiffPercent(nightDiffPercent);
            extracted.add("Night differential: " + dollars(nightDiffPercent) + "%");
        }

        Double weekendDiffPercent = detectPercentOrDollarDiff(text, draft.getOfferHourlyRate(), WEEKEND_PERCENT_PATTERN,
                WEEKEND_DOLLAR_PATTERN);
        if (weekendDiffPercent != null) {
            draft.setWeekendDiffPercent(weekendDiffPercent);
            extracted.add("Weekend differential: " + dollars(weekendDiffPercent) + "%");
        }

        Double signOnBonus = findMoneyNearKeywords(text,
                "sign-on", "sign on", "sob", "bonus", "retention bonus", "commencement bonus");
        if (signOnBonus == null) {
            signOnBonus = findMoneyAfterBonusLabel(text);
        }
        if (signOnBonus != null && signOnBonus < 250) {
            result.setParseWarning(mergeWarnings(result.getParseWarning(),
                    "Potential OCR issue: ignored an unusually low sign-on amount. Review the bonus manually."));
            signOnBonus = null;
        }
        if (signOnBonus != null) {
            draft.setSignOnBonus(signOnBonus);
            extracted.add("Sign-on bonus: $" + dollars(signOnBonus));
        }

        Double relocationStipend = findMoneyNearKeywords(text,
                "relo", "relocation", "relocation assistance", "relocation stipend", "moving reimbursement");
        if (relocationStipend == null) {
            relocationStipend = findMoneyBeforeRelocationLabel(text);
        }
        if (relocationStipend != null) {
            draft.setRelocationStipend(relocationStipend);
            extracted.add("Relocation support: $" + dollars(relocationStipend));
        }

        Double movingCost = findMoneyNearKeywords(text,
                "moving cost", "moving costs", "moving expense", "move cost", "move estimate");
        if (movingCost != null) {
            draft.setMovingCost(movingCost);
            extracted.add("Moving cost: $" + dollars(movingCost));
            foundMovingCost = true;
        }

        Integer contractMonths = findMonthsNearKeywords(text,
                "commitment", "contract", "employment term", "repayment period", "service period");
        if (contractMonths != null) {
            draft.setContractMonths(contractMonths);
            extracted.add("Commitment: " + contractMonths + " months");
        }

        Integer plannedStayMonths = findMonthsNearKeywords(text,
                "plan to stay", "planning to stay", "expected stay", "stay for", "leave after");
        if (plannedStayMonths != null) {
            draft.setPlannedStayMonths(plannedStayMonths);
            extracted.add("Planned stay: " + plannedStayMonths + " months");
            foundPlannedStay = true;
        }

        Optional<String> repaymentStyle = detectRepaymentStyle(lowerText);
        if (repaymentStyle.isPresent()) {
            draft.setRepaymentStyle(repaymentStyle.get());
            extracted.add("Repayment clause: " + readableRepaymentStyle(repaymentStyle.get()));
        }

        Double offerInsurance = findMoneyNearKeywords(text,
                "insurance premium", "health insurance", "medical premium", "employee premium", "benefits premium");
        if (offerInsurance != null) {
            draft.setOfferMonthlyInsurance(offerInsurance);
            extracted.add("Offer insurance premium: $" + dollars(offerInsurance) + "/mo");
        }

        Double currentInsurance = findMoneyNearKeywords(text,
                "current insurance", "current premium", "my insurance", "existing insurance");
        if (currentInsurance != null) {
            draft.setCurrentMonthlyInsurance(currentInsurance);
            extracted.add("Current insurance premium: $" + dollars(currentInsurance) + "/mo");
        }

        List<String> concernSignals = detectConcernSignals(lowerText);
        concernSignals.forEach(extracted::add);

        if (!foundOfferCity) {
            missing.add("Offer city");
        }
        if (!foundOfferRate) {
            missing.add("Offer hourly rate");
        }
        if (!jobPostMode) {
            if (!foundCurrentCity) {
                missing.add("Current city");
            }
            if (!foundCurrentRate) {
                missing.add("Current hourly rate");
            }
            if (draft.getContractMonths() > 0 && !foundPlannedStay) {
                missing.add("Planned stay months");
            }
            if (foundOfferCity && foundCurrentCity
                    && !draft.getOfferCitySlug().equalsIgnoreCase(draft.getCurrentCitySlug())
                    && !foundMovingCost) {
                missing.add("Moving cost estimate");
            }
        }

        result.setParsed(!extracted.isEmpty());
        result.setExtractedFields(new ArrayList<>(extracted));
        result.setMissingCriticalFields(missing);
        boolean operationalConcern = extracted.stream().anyMatch(field -> field.startsWith("Float terms:")
                || field.startsWith("Cancellation terms:"));
        if ((!concernSignals.isEmpty() || operationalConcern) && !missing.isEmpty()) {
            result.setSummary("Auto-filled " + extracted.size()
                    + " fields and found the concern. This is not enough for a final verdict yet. Confirm: "
                    + String.join(", ", missing) + ".");
            result.setParseWarning(mergeWarnings(result.getParseWarning(),
                    "Use the concern as the starting point, but do not trust the verdict until the missing basics are confirmed."));
        } else {
            result.setSummary(buildSummary(extracted.size(), missing, jobPostMode));
        }
        result.setEvidenceSnippets(extractEvidenceSnippets(text, draft));
        return result;
    }

    private Optional<String> detectRoleSlug(String lowerText) {
        if (containsApproximatePhrase(lowerText, "registered nurse", 6) || lowerText.contains(" rn ")
                || lowerText.startsWith("rn ") || lowerText.endsWith(" rn")) {
            return Optional.of("registered-nurse");
        }
        if (lowerText.contains("physical therapist") || lowerText.contains(" pt ")) {
            return Optional.of("physical-therapist");
        }
        if (lowerText.contains("pharmacist")) {
            return Optional.of("pharmacist");
        }
        if (lowerText.contains("resident physician") || lowerText.contains("medical resident")
                || lowerText.contains("resident doctor")) {
            return Optional.of("medical-resident");
        }
        return Optional.empty();
    }

    private Optional<String> detectUnitType(String lowerText) {
        if (containsUnitPhrase(lowerText, "icu") || containsUnitPhrase(lowerText, "critical care")) {
            return Optional.of("icu");
        }
        if (containsPhrase(lowerText, "er nurse") || containsPhrase(lowerText, "ed nurse")
                || containsUnitCode(lowerText, "er")
                || containsUnitCode(lowerText, "ed")
                || containsEmergencyDepartmentUnit(lowerText)) {
            return Optional.of("ed");
        }
        if (containsUnitPhrase(lowerText, "labor and delivery") || containsUnitPhrase(lowerText, "l&d")
                || containsUnitPhrase(lowerText, "mother baby")) {
            return Optional.of("l_and_d");
        }
        if (containsUnitPhrase(lowerText, "clinic") || containsUnitPhrase(lowerText, "outpatient")
                || containsUnitPhrase(lowerText, "ambulatory")) {
            return Optional.of("clinic");
        }
        if (containsUnitPhrase(lowerText, "med-surg") || containsUnitPhrase(lowerText, "med surg")
                || containsUnitPhrase(lowerText, "ms/tele") || containsUnitPhrase(lowerText, "ms tele")
                || containsUnitPhrase(lowerText, "m/s tele")
                || containsUnitPhrase(lowerText, "telemetry")) {
            return Optional.of("med_surg");
        }
        if (containsUnitPhrase(lowerText, "float pool") || containsFloatUnitCode(lowerText)) {
            return Optional.of("float_pool");
        }
        if (containsUnitPhrase(lowerText, "operating room") || containsUnitPhrase(lowerText, "perioperative")
                || containsOperatingRoomCode(lowerText)) {
            return Optional.of("or");
        }
        return Optional.empty();
    }

    private Optional<String> detectShiftGuarantee(String lowerText) {
        if (lowerText.contains("rotating shift") || lowerText.contains("rotate between")
                || lowerText.contains("rotates between")) {
            return Optional.of("rotating");
        }
        if (lowerText.contains("subject to change") || lowerText.contains("not guaranteed")
                || lowerText.contains("staffing needs") || lowerText.contains("manager discretion")
                || lowerText.contains("based on operational need")) {
            return Optional.of("unknown");
        }
        if (lowerText.contains("7p-7a") || lowerText.contains("7a-7p") || lowerText.contains("night shift")
                || lowerText.contains(" noc ") || lowerText.contains(" nocs ")
                || lowerText.contains("day shift") || lowerText.contains("weekend program")
                || lowerText.contains("3x12") || lowerText.contains("three 12")) {
            if (lowerText.contains("offer letter") || lowerText.contains("employment agreement")
                    || lowerText.contains("this offer")) {
                return Optional.of("written");
            }
            return Optional.of("verbal");
        }
        return Optional.empty();
    }

    private Optional<String> detectFloatRisk(String lowerText, String unitType) {
        if (lowerText.contains("hospital-wide float") || lowerText.contains("float to any unit")
                || lowerText.contains("across the hospital") || lowerText.contains("throughout the hospital")
                || lowerText.contains("float me everywhere") || lowerText.contains("floating everywhere")
                || "float_pool".equals(unitType)) {
            return Optional.of("hospital_wide");
        }
        if (lowerText.contains("adjacent units") || lowerText.contains("sister units")
                || lowerText.contains("within the service line") || lowerText.contains("neighboring units")) {
            return Optional.of("adjacent_units");
        }
        if (lowerText.contains("home unit only") || lowerText.contains("no float")
                || lowerText.contains("will not float")) {
            return Optional.of("home_unit_only");
        }
        return Optional.empty();
    }

    private Optional<String> detectCancelRisk(String lowerText) {
        if (lowerText.contains("guaranteed hours") || lowerText.contains("hours guaranteed")
                || lowerText.contains("gtd hours") || lowerText.contains("gtd hrs")
                || lowerText.contains("guar hours") || lowerText.contains("guar hrs")
                || lowerText.contains("not subject to cancellation")) {
            return Optional.of("protected_hours");
        }
        if (lowerText.contains("low census") || lowerText.contains("census drops")) {
            return Optional.of("low_census_only");
        }
        if (lowerText.contains("can cancel without pay") || lowerText.contains("hours not guaranteed")
                || lowerText.contains("subject to cancellation") || lowerText.contains("cancelled without pay")
                || lowerText.contains("may be canceled") || lowerText.contains("cancel me first")) {
            return Optional.of("can_cancel_without_pay");
        }
        return Optional.empty();
    }

    private Optional<String> detectRepaymentStyle(String lowerText) {
        if (lowerText.contains("prorat") || lowerText.contains("forgiven monthly")
                || lowerText.contains("earned monthly")) {
            return Optional.of("prorated");
        }
        if (lowerText.contains("no repayment") || lowerText.contains("not required to repay")
                || lowerText.contains("non-refundable")) {
            return Optional.of("none");
        }
        if (lowerText.contains("full repayment") || lowerText.contains("repay the full")
                || lowerText.contains("repay the entire") || lowerText.contains("100% repayment")) {
            return Optional.of("full");
        }
        return Optional.empty();
    }

    private List<String> detectConcernSignals(String lowerText) {
        List<String> signals = new ArrayList<>();
        if (containsAny(lowerText, "toxic culture", "bullying", "bully", "hostile culture",
                "unsafe culture", "lateral violence", "nurses eat their young", "bad culture", "taeum",
                "mean-girl", "mean girl", "cliquey", "dumpster fire")) {
            signals.add("Concern: unit culture / bullying risk");
        }
        if (containsAny(lowerText, "pay cut", "lower pay", "less money", "income went down",
                "income is lower", "take-home is lower", "take home is lower", "net pay is lower",
                "making less", "pays less", "make less after benefits", "move cost is bigger than the bonus")) {
            signals.add("Concern: lower take-home pay");
        }
        if (containsAny(lowerText, "short staffed", "short-staffed", "unsafe ratio", "unsafe ratios",
                "staffing ratio", "patient load", "no breaks", "no lunch", "too many patients",
                "ratios sound sketchy")) {
            signals.add("Concern: staffing / survivability risk");
        }
        if (containsAny(lowerText, "good coworkers", "good co-workers", "like my coworkers",
                "like my co-workers", "team seems good", "team is good", "coworkers are good",
                "co-workers are good", "manager seems good", "good manager", "good preceptor",
                "supportive team", "vibe check", "manager seems normal")) {
            signals.add("Positive tradeoff: team / support seems strong");
        }
        if (containsAny(lowerText, "closer to family", "near family", "better commute",
                "shorter commute", "school schedule", "childcare", "daycare", "kid pickup",
                "partner", "worse for my schedule", "better for my schedule")) {
            signals.add("Personal tradeoff: lifestyle or family fit matters");
        }
        return signals;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private CityAssignment detectCities(String lowerText) {
        List<CityMention> mentions = repository.getCities().stream()
                .map(city -> cityMentionFor(lowerText, city))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(CityMention::index))
                .toList();
        List<CityMention> contextualMentions = mentions.stream()
                .filter(mention -> mention.currentScore() > 0 || mention.offerScore() > 0)
                .toList();

        String currentSlug = null;
        String currentLabel = null;
        String offerSlug = null;
        String offerLabel = null;

        Optional<CityMention> currentMention = mentions.stream()
                .filter(mention -> mention.currentScore() > mention.offerScore() && mention.currentScore() > 0)
                .max(Comparator.comparingInt(CityMention::currentScore));
        Optional<CityMention> offerMention = mentions.stream()
                .filter(mention -> mention.offerScore() > mention.currentScore() && mention.offerScore() > 0)
                .max(Comparator.comparingInt(CityMention::offerScore));
        if (currentMention.isPresent()) {
            currentSlug = currentMention.get().slug();
            currentLabel = currentMention.get().label();
        }
        if (offerMention.isPresent()) {
            offerSlug = offerMention.get().slug();
            offerLabel = offerMention.get().label();
        }

        if (offerSlug == null && contextualMentions.isEmpty() && mentions.size() > 2) {
            return new CityAssignment(null, null, null, null);
        }

        if (offerSlug == null && mentions.size() == 1 && currentSlug == null) {
            CityMention only = mentions.get(0);
            offerSlug = only.slug();
            offerLabel = only.label();
        } else if (offerSlug == null && !mentions.isEmpty() && contextualMentions.isEmpty()) {
            CityMention last = mentions.get(mentions.size() - 1);
            offerSlug = last.slug();
            offerLabel = last.label();
        }

        CityMention explicitOfferCity = explicitOfferCity(mentions);
        if (explicitOfferCity != null) {
            offerSlug = explicitOfferCity.slug();
            offerLabel = explicitOfferCity.label();
        }
        CityMention explicitOfferCityFromText = explicitOfferCity(lowerText);
        if (explicitOfferCityFromText != null) {
            offerSlug = explicitOfferCityFromText.slug();
            offerLabel = explicitOfferCityFromText.label();
        }

        CityMention explicitCurrentCity = explicitCurrentCity(lowerText);
        if (explicitCurrentCity != null) {
            currentSlug = explicitCurrentCity.slug();
            currentLabel = explicitCurrentCity.label();
        }

        if (currentSlug == null && mentions.size() >= 2 && contextualMentions.isEmpty()) {
            String assignedOfferSlug = offerSlug;
            Optional<CityMention> firstNonOffer = mentions.stream()
                    .filter(mention -> !mention.slug().equals(assignedOfferSlug))
                    .findFirst();
            if (firstNonOffer.isPresent()) {
                currentSlug = firstNonOffer.get().slug();
                currentLabel = firstNonOffer.get().label();
            }
        }

        if (currentSlug == null && offerSlug != null) {
            String assignedOfferSlug = offerSlug;
            Optional<CityMention> contextualCurrent = mentions.stream()
                    .filter(mention -> mention.currentScore() > 0 && !mention.slug().equals(assignedOfferSlug))
                    .findFirst();
            if (contextualCurrent.isPresent()) {
                currentSlug = contextualCurrent.get().slug();
                currentLabel = contextualCurrent.get().label();
            }
        }

        return new CityAssignment(currentSlug, currentLabel, offerSlug, offerLabel);
    }

    private CityMention explicitOfferCity(List<CityMention> mentions) {
        return mentions.stream()
                .filter(mention -> mention.offerScore() >= 5)
                .max(Comparator.comparingInt(CityMention::offerScore))
                .orElse(null);
    }

    private CityMention explicitOfferCity(String lowerText) {
        CityMention cueMention = explicitOfferCityAfterCue(lowerText);
        if (cueMention != null) {
            return cueMention;
        }

        CityMention bestMention = null;
        for (CityCostEntry city : repository.getCities()) {
            for (String variant : cityVariants(city)) {
                CityMention candidate = explicitOfferCityForVariant(lowerText, city, variant);
                if (candidate != null && isBetterCityMention(candidate, bestMention)) {
                    bestMention = candidate;
                }
            }
        }
        return bestMention;
    }

    private CityMention explicitOfferCityAfterCue(String lowerText) {
        List<Pattern> cuePatterns = List.of(
                Pattern.compile("\\b(?:new\\s+offer\\s+city|offer\\s+city|recruiter\\s+offer\\s+located\\s+in|offer\\s+is\\s+located\\s+in|offer\\s+located\\s+in|position\\s+located\\s+in|job\\s+location|primary\\s+work\\s+location|open\\s+role|new\\s+rn\\s+job|new\\s+role|new\\s+job|offered)\\b\\s*(?:is|=|:)?\\s*(?:in|at)?\\s*",
                        Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\bnew\\s+(?:[a-z&/\\-]+\\s+){0,5}(?:rn|nurse|role|job)\\s+(?:in\\s+)?",
                        Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:registered\\s+nurse\\s+opening|nurse\\s+opening|rn\\s+opening|opening)\\s+in\\s+",
                        Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:pasted\\s+from\\s+pdf|job\\s+listing|job\\s+post(?:ing)?|copied\\s+listing)\\s*:?\\s*",
                        Pattern.CASE_INSENSITIVE));
        CityMention bestMention = null;
        for (Pattern cuePattern : cuePatterns) {
            Matcher cueMatcher = cuePattern.matcher(lowerText);
            while (cueMatcher.find()) {
                CityMention candidate = cityMentionAtCue(lowerText, cueMatcher.end());
                if (candidate != null && isBetterCityMention(candidate, bestMention)) {
                    bestMention = candidate;
                }
            }
        }
        return bestMention;
    }

    private CityMention cityMentionAtCue(String lowerText, int cueEnd) {
        String tail = lowerText.substring(cueEnd, Math.min(lowerText.length(), cueEnd + 60));
        CityMention bestMention = null;
        for (CityCostEntry city : repository.getCities()) {
            for (String variant : cityVariants(city)) {
                Matcher matcher = Pattern.compile("^\\s*" + Pattern.quote(variant) + "(?![a-z])",
                        Pattern.CASE_INSENSITIVE).matcher(tail);
                if (matcher.find()) {
                    CityMention candidate = new CityMention(city.getSlug(), city.getCity() + ", " + city.getState(),
                            cueEnd + matcher.start(), 0, 40);
                    if (isBetterCityMention(candidate, bestMention)) {
                        bestMention = candidate;
                    }
                }
            }
        }
        return bestMention;
    }

    private CityMention explicitOfferCityForVariant(String lowerText, CityCostEntry city, String variant) {
        String cityPattern = Pattern.quote(variant) + "(?![a-z])";
        List<Pattern> patterns = List.of(
                Pattern.compile("\\b(?:new\\s+offer\\s+city|offer\\s+city|offer|offered|new\\s+role|new\\s+job|new\\s+rn\\s+job|recruiter\\s+offer\\s+located\\s+in|offer\\s+located\\s+in|position\\s+located\\s+in|job\\s+location|primary\\s+work\\s+location|open\\s+role|location)\\s*(?:is|=|:)?\\s*(?:in|at)?\\s*"
                        + cityPattern, Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\bnew\\s+(?:[a-z&/\\-]+\\s+){0,5}(?:rn|nurse|role|job)\\s+(?:in\\s+)?"
                        + cityPattern, Pattern.CASE_INSENSITIVE),
                Pattern.compile("\\b(?:pasted\\s+from\\s+pdf|job\\s+listing|job\\s+post(?:ing)?|copied\\s+listing)\\s*:?\\s*"
                        + cityPattern + "\\s+(?:registered\\s+nurse|rn|nurse|icu|ed|er|med\\s+surg|med-surg|clinic|float|operating|labor)",
                        Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?<![a-z])" + cityPattern
                        + "\\s+(?:is\\s+offer|offer|new\\s+role|new\\s+job|med\\s+surg|med-surg|icu|ed|er|clinic|float\\s+pool|operating\\s+room|or\\s+rate|l&d|labor\\s+and\\s+delivery)",
                        Pattern.CASE_INSENSITIVE));
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(lowerText);
            if (matcher.find()) {
                return new CityMention(city.getSlug(), city.getCity() + ", " + city.getState(),
                        matcher.start(), 0, 30);
            }
        }
        return null;
    }

    private CityMention explicitCurrentCity(String lowerText) {
        for (CityCostEntry city : repository.getCities()) {
            for (String variant : cityVariants(city)) {
                Matcher matcher = Pattern.compile("\\b(?:currently|cur|rn\\s+now|me\\s+now|current\\s+job|current\\s+city|current(?:ly)?\\s+in|current(?:ly)?\\s+based\\s+in|coming\\s+from|i\\s+am\\s+in|i'm\\s+in)\\s+"
                        + Pattern.quote(variant) + "(?![a-z])", Pattern.CASE_INSENSITIVE).matcher(lowerText);
                if (matcher.find()) {
                    return new CityMention(city.getSlug(), city.getCity() + ", " + city.getState(),
                            matcher.start(), 20, 0);
                }
            }
        }
        return null;
    }

    private Optional<CityMention> cityMentionFor(String lowerText, CityCostEntry city) {
        CityMention bestMention = null;
        for (String variant : cityVariants(city)) {
            Matcher matcher = phrasePattern(variant).matcher(lowerText);
            while (matcher.find()) {
                Optional<CityMention> candidate = cityMentionAt(lowerText, city, matcher.start());
                if (candidate.isPresent() && isBetterCityMention(candidate.get(), bestMention)) {
                    bestMention = candidate.get();
                }
            }
        }

        return Optional.ofNullable(bestMention);
    }

    private List<String> cityVariants(CityCostEntry city) {
        List<String> variants = new ArrayList<>();
        variants.add((city.getCity() + ", " + city.getState()).toLowerCase(Locale.US));
        variants.add((city.getCity() + " " + city.getState()).toLowerCase(Locale.US));
        if (city.getCity().length() >= 5) {
            variants.add(city.getCity().toLowerCase(Locale.US));
        }
        if ("new-york-ny".equalsIgnoreCase(city.getSlug())) {
            variants.add("new york city");
            variants.add("nyc");
        }
        if ("los-angeles-ca".equalsIgnoreCase(city.getSlug())) {
            variants.add("los angeles");
            variants.add("la");
        }
        return variants;
    }

    private Optional<CityMention> cityMentionAt(String lowerText, CityCostEntry city, int bestIndex) {
        String broadContext = snippet(lowerText, bestIndex, 90);
        String before = localBefore(lowerText, bestIndex, 70);
        String after = localAfter(lowerText, bestIndex, 90);
        String context = before + " " + after;
        if (context.contains("proximity to destinations") || context.contains(" miles")
                || context.contains(" population") || context.contains("median rent")
                || context.contains("cost of living")) {
            return Optional.empty();
        }
        int offerScore = OFFER_CONTEXT.matcher(context).find() ? 2 : 0;
        int currentScore = CURRENT_CONTEXT.matcher(context).find() ? 3 : 0;
        int arrowIndex = lowerText.indexOf("->");
        if (arrowIndex >= 0) {
            if (bestIndex < arrowIndex) {
                currentScore += 6;
            } else {
                offerScore += 6;
            }
        }
        if (containsAny(context, "job location", "primary work location", "open role", "offer first",
                "offer:", "offer/", "new:", "new role", "new role/", "new job", "new rn job", "job post",
                "job posting", "position located", "located in", "posted pay", "base rate")) {
            offerScore += 5;
        }
        int beforeOfferIndex = lastKeywordIndex(before, "offer", "offered", "new role", "new job",
                "new rn job", "new thing", "new:", "job location", "primary work location", "offer city",
                "open role", "location", "located in");
        int beforeCurrentIndex = lastKeywordIndex(before, "current", "currently", "me now", "now/",
                "now =", "old:", "old job", "old rn job", "existing job", "my current");
        if (beforeOfferIndex >= 0 && beforeOfferIndex > beforeCurrentIndex) {
            offerScore += 8;
        }
        if (containsAny(after, " offer", " rn role", " role", " job post", " job posting", " opening",
                " base rate", " pays ", " at $")) {
            offerScore += 6;
        }
        if (containsAny(after, " is offer", " offer ", " new role", " new job")) {
            offerScore += 8;
        }
        if (containsAny(after, " is current", " today")) {
            currentScore += 10;
            offerScore = 0;
        }
        if (containsAny(after, " is offer")) {
            offerScore += 10;
            currentScore = 0;
        }
        if (containsAny(after, " ed", " icu", " rn", " nurse", " med surg", " med-surg", " ms/tele",
                " operating room", " or ", " clinic", " float pool")
                && containsAny(broadContext, "offer", "offered", "base rate", "/hr", "per hour")) {
            offerScore += 4;
        }
        if (containsAny(context, "current job", "current:", "currently", "me now", "now/", "now =", "old:",
                "old job", "old rn job", "existing job")) {
            currentScore += 5;
        }
        if (containsAny(before, "cur ") || containsAny(after, " is current", " today", " now")) {
            currentScore += 8;
        }
        if (containsAny(context, "coming from")) {
            currentScore += 5;
        }
        if (beforeCurrentIndex >= 0 && beforeCurrentIndex >= beforeOfferIndex) {
            currentScore += 8;
        }
        if (containsAny(after, " now", " current", " making ")) {
            currentScore += 2;
        }
        int nearIndex = before.lastIndexOf("near ");
        int currentlyIndex = before.lastIndexOf("currently");
        if (nearIndex >= 0 && nearIndex > currentlyIndex) {
            currentScore = 0;
        }
        if (offerScore > 0 && !hasExplicitCurrentCityContext(context)) {
            currentScore = 0;
        }
        return Optional.of(new CityMention(city.getSlug(), city.getCity() + ", " + city.getState(),
                bestIndex, currentScore, offerScore));
    }

    private boolean isBetterCityMention(CityMention candidate, CityMention current) {
        if (current == null) {
            return true;
        }
        int candidateStrength = Math.max(candidate.currentScore(), candidate.offerScore());
        int currentStrength = Math.max(current.currentScore(), current.offerScore());
        if (candidateStrength != currentStrength) {
            return candidateStrength > currentStrength;
        }
        if (candidate.offerScore() != current.offerScore()) {
            return candidate.offerScore() > current.offerScore();
        }
        return candidate.index() < current.index();
    }

    private boolean hasExplicitCurrentCityContext(String context) {
        return containsAny(context, "current", "currently", "existing", "today", "old:", "old job", "old rn job",
                "my current", "i make", "i earn", "making", "now", "live in", "work in", "living in",
                "working in", "based in");
    }

    private String localBefore(String text, int index, int radius) {
        String before = text.substring(Math.max(0, index - radius), index);
        int delimiter = Math.max(Math.max(before.lastIndexOf('.'), before.lastIndexOf(';')),
                Math.max(before.lastIndexOf('\n'), before.lastIndexOf('|')));
        return delimiter >= 0 ? before.substring(delimiter + 1) : before;
    }

    private String localAfter(String text, int index, int radius) {
        String after = text.substring(index, Math.min(text.length(), index + radius));
        int delimiter = firstDelimiter(after);
        return delimiter >= 0 ? after.substring(0, delimiter) : after;
    }

    private int firstDelimiter(String text) {
        int best = -1;
        for (char delimiter : new char[] {'.', ';', '\n', '|', '>'}) {
            int found = text.indexOf(delimiter);
            if (found >= 0 && (best < 0 || found < best)) {
                best = found;
            }
        }
        return best;
    }

    private HourlyAssignment detectHourlyRates(String text, String lowerText, boolean jobPostMode) {
        HourlyRange range = detectHourlyRange(text, lowerText, jobPostMode);
        List<NumericMention> matches = new ArrayList<>(findNumericMentions(text, HOURLY_RATE_PATTERN));
        matches.addAll(findNumericMentions(text, LABELED_HOURLY_RATE_PATTERN));
        matches.sort(Comparator.comparingInt(NumericMention::index));
        Double currentRate = null;
        Double offerRate = range != null ? range.selectedRate() : null;
        List<Double> generic = new ArrayList<>();

        for (NumericMention match : matches) {
            if (range != null && range.contains(match.value())) {
                continue;
            }
            String beforeRate = lowerText.substring(Math.max(0, match.index() - 32), match.index());
            if (beforeRate.contains("premium") || beforeRate.contains("differential")
                    || beforeRate.contains(" diff") || beforeRate.contains("shift premium")) {
                continue;
            }
            if (currentRate == null && isCurrentHourlyContext(lowerText, match.index())) {
                currentRate = match.value();
                continue;
            }
            if (offerRate == null && isOfferHourlyContext(lowerText, match.index())) {
                offerRate = match.value();
                continue;
            }
            int currentScore = contextScore(lowerText, match.index(), CURRENT_CONTEXT);
            int offerScore = contextScore(lowerText, match.index(), OFFER_CONTEXT);
            if (currentRate == null && currentScore > offerScore) {
                currentRate = match.value();
            } else if (offerRate == null && offerScore >= currentScore && offerScore > 0) {
                offerRate = match.value();
            } else {
                generic.add(match.value());
            }
        }

        if (offerRate == null && !generic.isEmpty()) {
            offerRate = generic.get(generic.size() - 1);
        }

        if (!jobPostMode && currentRate == null && generic.size() >= 2) {
            currentRate = generic.get(0);
        }

        return new HourlyAssignment(
                currentRate,
                offerRate,
                range != null && range.highRate() > range.lowRate(),
                range != null ? range.selectionLabel() : null,
                range != null ? range.selectionWarning() : null);
    }

    private HourlyRange detectHourlyRange(String text, String lowerText, boolean jobPostMode) {
        List<HourlyRange> ranges = new ArrayList<>();
        HourlyRange separatedRange = detectSeparatedHourlyRange(text, lowerText);
        if (separatedRange != null) {
            ranges.add(separatedRange);
        }
        ranges.addAll(findHourlyRanges(text, lowerText, HOURLY_RANGE_PATTERN));
        ranges.addAll(findHourlyRanges(text, lowerText, PAY_RANGE_MIN_MAX_PATTERN));
        if (ranges.isEmpty()) {
            return null;
        }

        ranges.sort(Comparator.comparingInt(HourlyRange::offerScore).reversed()
                .thenComparingInt(HourlyRange::currentScore)
                .thenComparingInt(HourlyRange::index));

        HourlyRange chosen = ranges.stream()
                .filter(range -> range.offerScore() >= range.currentScore())
                .findFirst()
                .orElse(ranges.get(0));

        if (chosen.highRate() <= chosen.lowRate()) {
            return null;
        }

        double selectedRate = jobPostMode
                ? chosen.lowRate()
                : (chosen.lowRate() + chosen.highRate()) / 2.0;
        String selectionLabel = jobPostMode
                ? "Posted pay range: $" + dollars(chosen.lowRate()) + " - $" + dollars(chosen.highRate())
                        + "/hr (using floor $" + dollars(selectedRate) + "/hr)"
                : "Pay range in document: $" + dollars(chosen.lowRate()) + " - $" + dollars(chosen.highRate())
                        + "/hr (using midpoint $" + dollars(selectedRate) + "/hr)";
        String selectionWarning = jobPostMode
                ? "Posted pay range detected. Job-post screens use the conservative floor until a written offer locks the actual rate."
                : "Written pay range detected. Confirm the exact starting rate before trusting the model.";

        return new HourlyRange(chosen.lowRate(), chosen.highRate(), selectedRate, chosen.index(),
                chosen.currentScore(), chosen.offerScore(), selectionLabel, selectionWarning);
    }

    private HourlyRange detectSeparatedHourlyRange(String text, String lowerText) {
        Matcher lowMatcher = PAY_RANGE_LOW_PATTERN.matcher(text);
        Matcher highMatcher = PAY_RANGE_HIGH_PATTERN.matcher(text);
        if (!lowMatcher.find() || !highMatcher.find()) {
            return null;
        }

        double low = parseHourlyNumber(lowMatcher.group(1));
        double high = parseHourlyNumber(highMatcher.group(1));
        int index = Math.min(lowMatcher.start(), highMatcher.start());
        String context = snippet(lowerText, index, 90);
        int currentScore = CURRENT_CONTEXT.matcher(context).find() ? 2 : 0;
        int offerScore = OFFER_CONTEXT.matcher(context).find() ? 2 : 0;
        return new HourlyRange(Math.min(low, high), Math.max(low, high), Math.min(low, high), index,
                currentScore, offerScore, null, null);
    }

    private List<HourlyRange> findHourlyRanges(String text, String lowerText, Pattern pattern) {
        List<HourlyRange> ranges = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            double first = parseHourlyNumber(matcher.group(1));
            double second = parseHourlyNumber(matcher.group(2));
            double low = Math.min(first, second);
            double high = Math.max(first, second);
            String context = snippet(lowerText, matcher.start(), 80);
            int currentScore = CURRENT_CONTEXT.matcher(context).find() ? 2 : 0;
            int offerScore = OFFER_CONTEXT.matcher(context).find() ? 2 : 0;
            ranges.add(new HourlyRange(low, high, low, matcher.start(), currentScore, offerScore, null, null));
        }
        return ranges;
    }

    private Double detectWeeklyHours(String text) {
        Matcher hoursMatcher = HOURS_PATTERN.matcher(text);
        while (hoursMatcher.find()) {
            String context = snippet(text.toLowerCase(Locale.US), hoursMatcher.start(), 45);
            if (containsAny(context, "shift", "shifts") && !containsAny(context, "per week", "/week", "/wk", "weekly")) {
                continue;
            }
            return Double.parseDouble(hoursMatcher.group(1));
        }

        Matcher threeTwelves = THREE_TWELVES_PATTERN.matcher(text);
        if (threeTwelves.find()) {
            return 36.0;
        }

        Matcher percentFteMatcher = PERCENT_FTE_PATTERN.matcher(text);
        if (percentFteMatcher.find()) {
            double percent = Double.parseDouble(percentFteMatcher.group(1));
            if (percent > 0 && percent <= 100) {
                return (percent / 100.0) * 40.0;
            }
        }

        Matcher fteMatcher = FTE_PATTERN.matcher(text);
        if (fteMatcher.find()) {
            double fte = Double.parseDouble(fteMatcher.group(1));
            return fte * 40.0;
        }

        return null;
    }

    private Double detectPercentOrDollarDiff(String text,
            double offerHourlyRate,
            Pattern percentPattern,
            Pattern dollarPattern) {
        Matcher percentMatcher = percentPattern.matcher(text);
        if (percentMatcher.find()) {
            return Double.parseDouble(percentMatcher.group(1));
        }

        Matcher dollarMatcher = dollarPattern.matcher(text);
        if (dollarMatcher.find() && offerHourlyRate > 0) {
            double dollarDiff = Double.parseDouble(dollarMatcher.group(1));
            return (dollarDiff / offerHourlyRate) * 100.0;
        }

        return null;
    }

    private Double findMoneyNearKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            MoneyCandidate best = null;
            Matcher keywordMatcher = keywordPattern(keyword).matcher(text);
            while (keywordMatcher.find()) {
                int keywordStart = keywordMatcher.start();
                int keywordEnd = keywordMatcher.end();
                int beforeStart = Math.max(0, keywordStart - 48);
                Matcher beforeMatcher = MONEY_TOKEN_PATTERN.matcher(text.substring(beforeStart, keywordStart));
                while (beforeMatcher.find()) {
                    int absoluteStart = beforeStart + beforeMatcher.start();
                    int absoluteEnd = beforeStart + beforeMatcher.end();
                    if (isHourlyMoneyToken(text, absoluteEnd)) {
                        continue;
                    }
                    String between = text.substring(beforeStart + beforeMatcher.end(), keywordStart);
                    String beforeTokenContext = moneyLabelContextBefore(text, absoluteStart);
                    if (between.matches("[^\\n\\r\\d$.,;:/]{0,28}")
                            && !hasConflictingMoneyLabel(keyword, beforeTokenContext + between)) {
                        best = closerMoney(best, parseMoneyToken(beforeMatcher.group()),
                                keywordStart - absoluteEnd);
                    }
                }

                int afterEnd = Math.min(text.length(), keywordEnd + 48);
                Matcher afterMatcher = MONEY_TOKEN_PATTERN.matcher(text.substring(keywordEnd, afterEnd));
                while (afterMatcher.find()) {
                    int absoluteStart = keywordEnd + afterMatcher.start();
                    int absoluteEnd = keywordEnd + afterMatcher.end();
                    if (isHourlyMoneyToken(text, absoluteEnd)) {
                        continue;
                    }
                    String between = text.substring(keywordEnd, keywordEnd + afterMatcher.start());
                    String afterTokenContext = moneyLabelContextAfter(text, absoluteEnd);
                    if (between.matches("[^\\n\\r.,;]{0,40}?")
                            && !hasConflictingMoneyLabel(keyword, between + afterTokenContext)) {
                        best = closerMoney(best, parseMoneyToken(afterMatcher.group()), afterMatcher.start());
                    }
                }
            }
            if (best != null) {
                return best.value();
            }
        }
        return null;
    }

    private Double findMoneyAfterBonusLabel(String text) {
        Pattern pattern = Pattern.compile(
                "(?i)\\b(?:sign-?on|sob|retention\\s+bonus|commencement\\s+bonus|bonus)"
                        + "(?:\\s+bonus|\\s+amount)?(?:\\s+(?:maybe|about|around|approximately))?\\s*[:=]?\\s*"
                        + MONEY_CAPTURE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? parseMoneyToken(matcher.group(1)) : null;
    }

    private Double findMoneyBeforeRelocationLabel(String text) {
        Pattern pattern = Pattern.compile(MONEY_CAPTURE
                + "\\s+(?:relo|relocation(?:\\s+(?:stipend|assistance|support))?|moving\\s+reimbursement)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String before = moneyLabelContextBefore(text, matcher.start());
            if (!hasConflictingMoneyLabel("relocation", before)) {
                return parseMoneyToken(matcher.group(1));
            }
        }
        return null;
    }

    private boolean hasConflictingMoneyLabel(String keyword, String between) {
        String lowerKeyword = keyword.toLowerCase(Locale.US);
        String lowerBetween = between.toLowerCase(Locale.US);
        boolean relocationKeyword = lowerKeyword.contains("relo") || lowerKeyword.contains("moving");
        boolean bonusKeyword = lowerKeyword.contains("bonus") || lowerKeyword.contains("sign") || lowerKeyword.equals("sob");
        if (relocationKeyword && containsAny(lowerBetween, "bonus", "sign-on", "sign on", "sob", "retention",
                "tuition", "parking", "do not count", "not count")) {
            return true;
        }
        return bonusKeyword && containsAny(lowerBetween, "relo", "relocation", "moving");
    }

    private String moneyLabelContextBefore(String text, int index) {
        String before = text.substring(Math.max(0, index - 32), index);
        int delimiter = Math.max(Math.max(Math.max(before.lastIndexOf(','), before.lastIndexOf(';')),
                Math.max(before.lastIndexOf('.'), before.lastIndexOf('\n'))),
                Math.max(Math.max(before.lastIndexOf('|'), before.lastIndexOf('+')),
                        Math.max(before.lastIndexOf('-'), before.lastIndexOf('/'))));
        String label = delimiter >= 0 ? before.substring(delimiter + 1) : before;
        int andIndex = label.toLowerCase(Locale.US).lastIndexOf(" and ");
        return andIndex >= 0 ? label.substring(andIndex + 5) : label;
    }

    private String moneyLabelContextAfter(String text, int index) {
        String after = text.substring(index, Math.min(text.length(), index + 36));
        int delimiter = firstDelimiter(after);
        int comma = after.indexOf(',');
        if (comma >= 0 && (delimiter < 0 || comma < delimiter)) {
            delimiter = comma;
        }
        return delimiter >= 0 ? after.substring(0, delimiter) : after;
    }

    private Pattern keywordPattern(String keyword) {
        return Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(keyword) + "(?![A-Za-z0-9])",
                Pattern.CASE_INSENSITIVE);
    }

    private boolean isHourlyMoneyToken(String text, int tokenEnd) {
        String suffix = text.substring(tokenEnd, Math.min(text.length(), tokenEnd + 16));
        return HOURLY_SUFFIX_PATTERN.matcher(suffix).find();
    }

    private MoneyCandidate closerMoney(MoneyCandidate current, double value, int distance) {
        if (current == null || distance < current.distance()
                || (distance == current.distance() && value > current.value())) {
            return new MoneyCandidate(value, distance);
        }
        return current;
    }

    private Integer findMonthsNearKeywords(String text, String... keywords) {
        String lowerText = text.toLowerCase(Locale.US);
        for (String keyword : keywords) {
            Matcher keywordMatcher = keywordPattern(keyword).matcher(lowerText);
            while (keywordMatcher.find()) {
                int keywordStart = keywordMatcher.start();
                int keywordEnd = keywordMatcher.end();
                boolean plannedStayKeyword = keyword.contains("stay") || keyword.contains("leave");
                int beforeStart = Math.max(0, keywordStart - 48);
                Matcher beforeMatcher = MONTHS_PATTERN.matcher(text.substring(beforeStart, keywordStart));
                Integer closestBefore = null;
                while (beforeMatcher.find()) {
                    closestBefore = Integer.parseInt(beforeMatcher.group(1));
                }
                Matcher afterMatcher = MONTHS_PATTERN.matcher(text.substring(keywordEnd,
                        Math.min(text.length(), keywordEnd + 48)));
                Integer closestAfter = afterMatcher.find() ? Integer.parseInt(afterMatcher.group(1)) : null;
                if (plannedStayKeyword && closestAfter != null) {
                    return closestAfter;
                }
                if (closestBefore != null) {
                    return closestBefore;
                }
                if (closestAfter != null) {
                    return closestAfter;
                }
            }
        }
        return null;
    }

    private List<NumericMention> findNumericMentions(String text, Pattern pattern) {
        List<NumericMention> mentions = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            mentions.add(new NumericMention(parseHourlyNumber(matcher.group(1)), matcher.start()));
        }
        return mentions;
    }

    private int contextScore(String lowerText, int index, Pattern pattern) {
        int beforeStart = Math.max(0, index - 90);
        String before = lowerText.substring(beforeStart, index);
        int afterEnd = Math.min(lowerText.length(), index + 48);
        String after = lowerText.substring(index, afterEnd);
        int score = 0;
        if (pattern.matcher(before).find()) {
            score += 3;
        }
        if (pattern.matcher(after).find()) {
            score += pattern == CURRENT_CONTEXT ? 2 : 1;
        }
        return score;
    }

    private boolean isCurrentHourlyContext(String lowerText, int index) {
        String before = lowerText.substring(Math.max(0, index - 80), index);
        String after = lowerText.substring(index, Math.min(lowerText.length(), index + 32));
        int currentIndex = lastKeywordIndex(before, "current", "currently", "me now", "now/", "now =",
                "old:", "old job", "old rn job", "existing job", "i make", "i earn", "making");
        int offerIndex = lastKeywordIndex(before, "offer", "offered", "new thing", "new role", "new job",
                "new rn job", "new:", "base rate", "role", "posting");
        return currentIndex >= 0 && currentIndex >= offerIndex
                || (after.contains(" now") && offerIndex < 0);
    }

    private boolean isOfferHourlyContext(String lowerText, int index) {
        String before = lowerText.substring(Math.max(0, index - 80), index);
        int currentIndex = lastKeywordIndex(before, "current", "currently", "me now", "now/", "old:",
                "old job", "old rn job", "existing job", "i make", "i earn", "making");
        int offerIndex = lastKeywordIndex(before, "offer", "offered", "new thing", "new role", "new job",
                "new rn job", "new:", "base rate", "role", "posting", "nurse job", "opening", "compensation");
        return offerIndex >= 0 && offerIndex > currentIndex
                || before.contains("from recruiter") && before.contains("new ");
    }

    private int lastKeywordIndex(String text, String... keywords) {
        int best = -1;
        for (String keyword : keywords) {
            best = Math.max(best, text.lastIndexOf(keyword));
        }
        return best;
    }

    private List<NumericMention> findMoneyMentions(String text) {
        List<NumericMention> mentions = new ArrayList<>();
        Matcher matcher = MONEY_TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            double value = parseMoneyToken(matcher.group());
            if (value > 0) {
                mentions.add(new NumericMention(value, matcher.start()));
            }
        }
        return mentions;
    }

    private double parseMoneyToken(String token) {
        String normalized = token.replace("$", "").replace(",", "").trim().toLowerCase(Locale.US);
        boolean isThousands = normalized.endsWith("k");
        if (isThousands) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        double base = Double.parseDouble(normalized);
        return isThousands ? base * 1000.0 : base;
    }

    private int findPhrase(String text, String phrase) {
        Matcher matcher = phrasePattern(phrase).matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private Pattern phrasePattern(String phrase) {
        return Pattern.compile("(?<![a-z])" + Pattern.quote(phrase) + "(?![a-z])",
                Pattern.CASE_INSENSITIVE);
    }

    private boolean containsApproximatePhrase(String text, String phrase, int maxTrim) {
        if (findPhrase(text, phrase) >= 0) {
            return true;
        }
        int trimLimit = Math.min(maxTrim, Math.max(0, phrase.length() - 4));
        for (int trim = 1; trim <= trimLimit; trim++) {
            String truncated = phrase.substring(trim);
            Matcher matcher = Pattern.compile("(?<![a-z])(?:[\\p{Punct}]?\\s*)?" + Pattern.quote(truncated) + "(?![a-z])",
                    Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                return true;
            }
        }
        return false;
    }

    private double parseHourlyNumber(String token) {
        return Double.parseDouble(token.replace(",", "").trim());
    }

    private boolean containsPhrase(String text, String phrase) {
        return findPhrase(text, phrase) >= 0;
    }

    private boolean containsUnitPhrase(String text, String phrase) {
        int index = findPhrase(text, phrase);
        if (index < 0) {
            return false;
        }
        String context = snippet(text, index, 90);
        return containsJobScopedPhrase(text, phrase)
                || containsAny(context, "unit", "department", "dept", "job listing", "open role", "position",
                        "posted pay", "base rate", "compensation", "hourly", "/hr", "per hour", "registered nurse",
                        " rn", " nurse");
    }

    private boolean containsEmergencyDepartmentUnit(String text) {
        int index = findPhrase(text, "emergency department");
        if (index < 0) {
            return false;
        }
        String context = snippet(text, index, 120);
        if (containsAny(context, "freestanding emergency department", "affiliated entities",
                "proximity to destinations", "includes a freestanding", "other affiliated")) {
            return false;
        }
        return containsUnitPhrase(text, "emergency department");
    }

    private boolean containsUnitCode(String text, String code) {
        Matcher matcher = Pattern.compile("(?<![a-z])" + Pattern.quote(code) + "(?![a-z])",
                Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            String context = snippet(text, matcher.start(), 70);
            if (containsAny(context, "unit", "department", "dept", "role", "position", "offer", "offered",
                    "new job", "new role", "posting", "listing", "posted pay", "pay range", "base rate", "/hr",
                    "hourly", "rn", "nurse", "shift", "nocs")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFloatUnitCode(String text) {
        Matcher matcher = Pattern.compile("(?<![a-z])float(?![a-z])", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            String context = snippet(text, matcher.start(), 70);
            if (containsAny(context, "hospital-wide float", "within service line", "float within", "float to any")) {
                continue;
            }
            if (containsAny(context, "unit", "department", "dept", "role", "position", "offer", "offered",
                    "new job", "new role", "posting", "listing", "base rate", "/hr", "hourly", "rn", "nurse")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsOperatingRoomCode(String text) {
        Matcher matcher = Pattern.compile("(?<![a-z])or(?![a-z])", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            String before = text.substring(Math.max(0, matcher.start() - 16), matcher.start());
            String after = text.substring(matcher.end(), Math.min(text.length(), matcher.end() + 32));
            if (before.endsWith(", ") && !containsAny(before, "$", "/hr", "per hour")) {
                continue;
            }
            if (containsAny(after, " registered nurse", " rn", " unit", " role", " position", " rate", " hourly",
                    " $", " offer", " located")
                    || containsAny(before, "unit ", "unit=", "department ", "dept ", "dept=", "code ")
                    || ((after.startsWith(".") || after.startsWith(";") || after.startsWith(","))
                            && containsAny(before, "offer", "$", "/hr", "per hour", "unit=", "dept=", "->"))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsJobScopedPhrase(String text, String phrase) {
        int index = findPhrase(text, phrase);
        if (index < 0) {
            return false;
        }
        String context = snippet(text, index, 80);
        return containsApproximatePhrase(context, "registered nurse", 6)
                || Pattern.compile("(?<![a-z])rn(?![a-z])").matcher(context).find()
                || context.startsWith("rn ")
                || context.contains(" nurse ")
                || context.contains(" shift")
                || context.contains(" unit")
                || context.contains(" position")
                || context.contains(" opportunity")
                || context.contains(" offer")
                || context.contains(" role")
                || context.contains(" opening")
                || context.contains(" posting")
                || context.contains(" dept")
                || context.contains(" job")
                || context.contains(" fte");
    }

    private String normalize(String sourceText) {
        if (sourceText == null) {
            return "";
        }
        return sourceText.replace('\u00A0', ' ')
                .replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replaceAll("\\r\\n?", "\n")
                .replaceAll("(?i)/\\s*hourly\\b", " hourly")
                .replaceAll("(?i)l0cati0n", "location")
                .replaceAll("(?i)rel0cation", "relocation")
                .replaceAll("(?i)s1gn-on", "sign-on")
                .replaceAll("(?i)n1ght", "night")
                .replaceAll("(?i)night\\s+(?:shit|shin)\\s+premium", "night shift premium")
                .replaceAll("(?i)weekend\\s+shit\\s+premium", "weekend shift premium")
                .replaceAll("(?i)r0le", "role")
                .replaceAll("(?i)br0chure", "brochure")
                .replaceAll("(?i)benef1ts", "benefits")
                .replaceAll("(?i)\\bnyc\\b", "New York City")
                .replaceAll("(?i)\\bla\\b(?=\\s+(?:at|ed|icu|rn|nurse|offer|job|role|position|opening))",
                        "Los Angeles CA")
                .replaceAll("(?i)(sign-?on bonus|retention bonus|commencement bonus|bonus),\\s*maybe\\s*\\$",
                        "$1 maybe \\$")
                .replaceAll("(\\d+\\.\\d{2})\\.\\d{2}\\b", "$1")
                .replaceAll("[ \t]+", " ")
                .trim();
    }

    private String snippet(String text, int index, int radius) {
        int start = Math.max(0, index - radius);
        int end = Math.min(text.length(), index + radius);
        return text.substring(start, end);
    }

    private String buildSummary(int extractedCount, List<String> missing, boolean jobPostMode) {
        if (extractedCount == 0) {
            return jobPostMode
                    ? "No obvious listing fields were found. Paste a longer excerpt with pay, city, unit, or shift language."
                    : "No obvious nurse-offer fields were found. Paste a longer excerpt with pay, city, and contract language.";
        }
        if (missing.isEmpty()) {
            return jobPostMode
                    ? "Auto-filled " + extractedCount + " fields from the listing. Review the screen, then run it."
                    : "Auto-filled " + extractedCount + " fields from the pasted text. Review the numbers, then run the report.";
        }
        return jobPostMode
                ? "Auto-filled " + extractedCount + " fields. Before screening, check: " + String.join(", ", missing)
                        + "."
                : "Auto-filled " + extractedCount + " fields. Review these before running the report: "
                        + String.join(", ", missing) + ".";
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

    private List<String> extractEvidenceSnippets(String text, OfferRiskDraft draft) {
        List<String> snippets = new ArrayList<>();

        addEvidence(snippets, text, "sign-on", "sign on", "bonus");
        addEvidence(snippets, text, "relocation", "moving reimbursement", "moving cost");
        if (draft.getContractMonths() > 0 || !"none".equalsIgnoreCase(draft.getRepaymentStyle())) {
            addEvidence(snippets, text, "repay", "prorat", "commitment", "contract", "service period");
        }
        addEvidence(snippets, text, "shift", "7p-7a", "7a-7p", "night shift", "day shift", "rotating");
        addEvidence(snippets, text, "float");
        addEvidence(snippets, text, "cancel", "low census", "guaranteed hours");
        if (draft.getOfferMonthlyInsurance() > 0 || draft.getCurrentMonthlyInsurance() > 0) {
            addEvidence(snippets, text, "insurance", "premium", "benefits");
        }
        addEvidence(snippets, text, "/hr", "per hour", "hourly");

        if (snippets.isEmpty()) {
            for (String segment : splitEvidenceSegments(text)) {
                if (segment.length() >= 18) {
                    snippets.add(segment);
                }
                if (snippets.size() >= 3) {
                    break;
                }
            }
        }

        return snippets;
    }

    private void addEvidence(List<String> snippets, String text, String... keywords) {
        for (String segment : splitEvidenceSegments(text)) {
            String lowerSegment = segment.toLowerCase(Locale.US);
            for (String keyword : keywords) {
                if (lowerSegment.contains(keyword.toLowerCase(Locale.US))) {
                    if (!snippets.contains(segment)) {
                        snippets.add(segment);
                    }
                    if (snippets.size() >= 5) {
                        return;
                    }
                    break;
                }
            }
        }
    }

    private List<String> splitEvidenceSegments(String text) {
        List<String> segments = new ArrayList<>();
        String[] lines = text.split("\\n+");
        for (String rawLine : lines) {
            String line = rawLine.trim().replaceAll("\\s+", " ");
            if (line.length() >= 18) {
                segments.add(trimEvidence(line));
            }
        }
        if (!segments.isEmpty()) {
            return segments;
        }

        String[] sentences = text.split("(?<=[.!?;])\\s+");
        for (String rawSentence : sentences) {
            String sentence = rawSentence.trim().replaceAll("\\s+", " ");
            if (sentence.length() >= 18) {
                segments.add(trimEvidence(sentence));
            }
        }
        return segments;
    }

    private String trimEvidence(String segment) {
        if (segment.length() <= 180) {
            return segment;
        }
        return segment.substring(0, 177).trim() + "...";
    }

    private String readableRole(String roleSlug) {
        return switch (roleSlug) {
            case "physical-therapist" -> "Physical Therapist";
            case "pharmacist" -> "Pharmacist";
            case "medical-resident" -> "Medical Resident";
            default -> "Registered Nurse";
        };
    }

    private String readableUnit(String unitType) {
        return switch (unitType) {
            case "icu" -> "ICU / critical care";
            case "ed" -> "Emergency department";
            case "or" -> "Operating room";
            case "l_and_d" -> "Labor and delivery";
            case "clinic" -> "Clinic / outpatient";
            case "float_pool" -> "Float pool";
            case "other" -> "Other unit";
            default -> "Med-surg / telemetry";
        };
    }

    private String readableShiftGuarantee(String value) {
        return switch (value) {
            case "verbal" -> "Shift discussed but not written";
            case "rotating" -> "Rotating shift";
            case "unknown" -> "Shift not confirmed";
            default -> "Written shift schedule";
        };
    }

    private String readableFloatRisk(String value) {
        return switch (value) {
            case "adjacent_units" -> "Float to adjacent units";
            case "hospital_wide" -> "Hospital-wide float";
            case "unknown" -> "Float terms unknown";
            default -> "Home unit only";
        };
    }

    private String readableCancelRisk(String value) {
        return switch (value) {
            case "low_census_only" -> "Low-census cancellation possible";
            case "can_cancel_without_pay" -> "Can cancel without pay";
            case "unknown" -> "Cancellation terms unknown";
            default -> "Guaranteed scheduled hours";
        };
    }

    private String readableRepaymentStyle(String value) {
        return switch (value) {
            case "full" -> "Full repayment";
            case "none" -> "No repayment";
            default -> "Prorated repayment";
        };
    }

    private String dollars(double value) {
        if (Math.floor(value) == value) {
            return String.format(Locale.US, "%,.0f", value);
        }
        return String.format(Locale.US, "%,.2f", value);
    }

    private record CityMention(String slug, String label, int index, int currentScore, int offerScore) {
    }

    private record CityAssignment(String currentCitySlug,
                                  String currentCityLabel,
                                  String offerCitySlug,
                                  String offerCityLabel) {
    }

    private record NumericMention(double value, int index) {
    }

    private record MoneyCandidate(double value, int distance) {
    }

    private record HourlyAssignment(Double currentRate,
                                    Double offerRate,
                                    boolean rangeDetected,
                                    String selectionLabel,
                                    String selectionWarning) {
    }

    private record HourlyRange(double lowRate,
                               double highRate,
                               double selectedRate,
                               int index,
                               int currentScore,
                               int offerScore,
                               String selectionLabel,
                               String selectionWarning) {
        private boolean contains(double value) {
            return Math.abs(value - lowRate) < 0.01 || Math.abs(value - highRate) < 0.01;
        }
    }
}
