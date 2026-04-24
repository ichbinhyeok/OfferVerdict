package com.offerverdict.controller;

import com.offerverdict.exception.ResourceNotFoundException;
import com.offerverdict.service.ComparisonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class NurseOfferIssueController {
    private static final List<String> SEED_PATHS = List.of(
            "/rn-offer-red-flags",
            "/should-i-accept-nurse-job-offer",
            "/should-i-sign-nurse-job-offer",
            "/nurse-job-offer-review",
            "/nurse-offer-letter-red-flags",
            "/nurse-offer-before-signing-checklist",
            "/nurse-offer-life-fit",
            "/nurse-offer-family-relocation",
            "/nurse-night-shift-childcare-offer",
            "/nurse-offer-toxic-unit-culture",
            "/nurse-offer-negotiation-questions",
            "/compare-two-nurse-job-offers",
            "/rn-sign-on-bonus-clawback",
            "/rn-bonus-repayment-clause",
            "/rn-relocation-stipend-tax",
            "/nurse-relocation-package-worth-it",
            "/rn-guaranteed-hours-offer",
            "/rn-low-census-cancellation",
            "/rn-shift-guarantee-offer",
            "/rn-float-policy-offer",
            "/rn-float-pool-offer-red-flags",
            "/rn-pay-range-offer-letter",
            "/rn-night-shift-differential-offer",
            "/rn-weekend-holiday-requirement",
            "/rn-call-requirement-offer",
            "/rn-benefits-health-insurance-offer",
            "/rn-orientation-length-offer",
            "/rn-preceptor-support-offer",
            "/rn-staffing-ratio-offer",
            "/rn-unit-culture-red-flags",
            "/new-grad-nurse-offer-red-flags",
            "/travel-nurse-contract-red-flags",
            "/rn-prn-offer-red-flags",
            "/rn-internal-transfer-offer",
            "/icu-nurse-offer-red-flags",
            "/ed-nurse-offer-red-flags",
            "/med-surg-tele-nurse-offer-red-flags",
            "/labor-delivery-nurse-offer-red-flags");

    private static final List<UnitIntent> UNIT_INTENTS = List.of(
            unit("icu", "ICU", "high acuity, vasoactive drips, respiratory instability, and fast escalation needs",
                    "What ICU orientation, charge backup, and critical-care float boundary are written into the offer?",
                    "high-acuity support has to be explicit before the pay is trusted"),
            unit("ed", "ED", "boarding, hallway care, psych holds, triage pressure, and violence exposure",
                    "How does the ED handle boarding, security, triage load, and unsafe assignment escalation?",
                    "the offer has to account for boarding, safety, and throughput pressure"),
            unit("med-surg-tele", "Med-surg tele", "high patient turnover, telemetry burden, total-care load, and variable CNA support",
                    "What is the normal assignment, admit/discharge load, tele burden, and CNA support on nights?",
                    "workload support matters as much as the listed patient ratio"),
            unit("labor-delivery", "Labor and delivery", "induction volume, OB triage, C-section recovery, fetal monitoring, and hemorrhage readiness",
                    "What L&D coverage expectations, triage role, OR/PACU coverage, and neonatal support are written down?",
                    "coverage expectations and fetal/maternal support have to be clear"),
            unit("or", "OR", "call burden, specialty service lines, room turnover, scrub/circulate expectations, and late cases",
                    "Which services, call frequency, response time, and orientation path are guaranteed?",
                    "call and service-line expectations have to be priced into the decision"),
            unit("pacu", "PACU", "airway risk, phase I recovery, overflow boarding, call, and rapid deterioration",
                    "What acuity, call, airway support, and overflow expectations apply on this PACU?",
                    "recovery acuity and backup have to match the role"),
            unit("nicu", "NICU", "neonatal acuity, feeder-grower mix, respiratory support, delivery attendance, and family support",
                    "What NICU level, orientation length, delivery attendance, and respiratory backup are defined?",
                    "neonatal acuity requires written support and orientation depth"),
            unit("picu", "PICU", "pediatric critical care acuity, family dynamics, respiratory support, and transport/rapid response exposure",
                    "What PICU acuity, preceptor model, charge coverage, and float boundary are written?",
                    "pediatric critical-care support cannot be assumed"),
            unit("psych", "Psych", "behavioral escalation, violence risk, seclusion/restraint policy, security, and staffing mix",
                    "What security coverage, staffing mix, and escalation support exist on the psych unit?",
                    "behavioral safety support must be explicit"),
            unit("oncology", "Oncology", "chemo competency, neutropenic precautions, symptom burden, family support, and infusion complexity",
                    "What chemo training, certification support, and acuity mix are part of orientation?",
                    "specialty competency and safe onboarding need to be written"),
            unit("dialysis", "Dialysis", "patient volume, on-call burden, water system issues, inpatient acuity, and treatment timing",
                    "What patient load, call requirement, inpatient coverage, and training path apply?",
                    "treatment volume and call burden have to be real, not implied"),
            unit("home-health", "Home health", "drive time, visit volume, documentation burden, mileage, territory, and on-call rotation",
                    "What territory, visit expectations, mileage, documentation time, and call rotation are written?",
                    "territory and unpaid time can change the economics of the offer"));

    private static final List<RiskIntent> RISK_INTENTS = List.of(
            risk("sign-on-bonus-clawback", "sign-on bonus clawback", "repayment risk",
                    "Bonus money is not upside until the repayment terms are survivable.",
                    "A large bonus can hide a weak unit or a broad repayment trigger.",
                    "Check whether bonus money becomes a repayment trap.",
                    List.of("The bonus is full-payback instead of prorated.", "Repayment triggers after unit, shift, or employer-driven changes.", "The commitment period is longer than your realistic stay."),
                    List.of("Is repayment prorated monthly?", "Does repayment apply if the employer ends or changes the role?", "Does a unit, shift, or location change trigger repayment?"),
                    "treat the bonus as risk, not income"),
            risk("bonus-repayment-clause", "bonus repayment clause", "repayment clause",
                    "The clause decides who carries the downside if the job fails.",
                    "A repayment clause can turn a good-looking offer into a locked-in downside.",
                    "Read repayment triggers before counting bonus upside.",
                    List.of("Repayment is due in full after any separation.", "Termination without cause is not excluded.", "Repayment language is broader than the bonus explanation."),
                    List.of("What separations trigger repayment?", "Is repayment forgiven over time?", "Can repayment be waived if the employer changes the role?"),
                    "do not accept broad repayment language without written limits"),
            risk("relocation-repayment-clause", "relocation repayment clause", "relocation lock-in",
                    "Relocation support can help you move and still make it expensive to leave.",
                    "The real question is whether the relocation terms protect you or lock you into a bad unit.",
                    "Check whether relocation support creates a second repayment trap.",
                    List.of("Relocation repayment is not prorated.", "The stipend is taxable, delayed, or lower than real move cost.", "Relocation and sign-on repayment stack together."),
                    List.of("Is relocation repayment separate from sign-on repayment?", "When is the stipend paid and taxed?", "What happens if the employer changes the role after you move?"),
                    "do not move on weak relocation protection"),
            risk("guaranteed-hours", "guaranteed hours", "pay certainty",
                    "The hourly rate only matters if the hours are protected.",
                    "Guaranteed hours decide whether the expected income is reachable after you sign.",
                    "Confirm the hours before trusting the rate.",
                    List.of("FTE is listed but weekly hours are not guaranteed.", "Low census can reduce pay without protection.", "Repayment obligations remain even if hours disappear."),
                    List.of("Are weekly hours guaranteed in writing?", "What happens during low census?", "Do cancelled hours affect bonus or relocation repayment?"),
                    "do not accept pay math that depends on unprotected hours"),
            risk("low-census-cancellation", "low census cancellation", "cancellation risk",
                    "A strong rate can fail if scheduled shifts can disappear.",
                    "Cancellation language shifts facility volume risk onto the nurse.",
                    "Check whether scheduled hours can disappear after you sign.",
                    List.of("The facility can cancel shifts without pay.", "Low census policy is missing from the offer.", "Cancellation can occur while repayment terms still apply."),
                    List.of("How many shifts can be cancelled per schedule period?", "Are cancelled hours paid or made up?", "Does cancellation affect guaranteed hours or repayment?"),
                    "do not accept facility risk without hour protection"),
            risk("float-policy", "float policy", "float risk",
                    "A broad float radius can change the job you are actually accepting.",
                    "Float language matters because it decides whether the offer is a home-unit role or a staffing-flex role.",
                    "Review whether the home unit is actually protected.",
                    List.of("The offer says hospital-wide float without unit names.", "Float crosses competency boundaries.", "Orientation for float areas is not described."),
                    List.of("What is the exact home unit?", "Which units and campuses are included?", "What float areas are excluded by competency?"),
                    "do not call it a home-unit offer if float scope is broad and vague"),
            risk("shift-guarantee", "shift guarantee", "schedule control",
                    "A verbal shift promise is not schedule control.",
                    "Shift language changes sleep, family life, commute, differential pay, and retention risk.",
                    "Make the schedule real before signing.",
                    List.of("Shift is verbal but missing from the offer.", "Rotation is allowed without limits.", "Weekends, holidays, or call are not defined."),
                    List.of("Is the shift guaranteed in writing?", "Can the employer rotate shifts after start?", "How are weekends, holidays, and call assigned?"),
                    "do not sign if the schedule was core to your yes but is not written"),
            risk("night-shift-differential", "night shift differential", "differential pay",
                    "Differential pay is upside only if the shift and eligibility are real.",
                    "Night and weekend math can make weak base pay look better than it is.",
                    "Separate real base pay from conditional upside.",
                    List.of("The base rate is weak without differential.", "Differential eligibility rules are missing.", "The schedule may rotate away from the expected premium."),
                    List.of("What is the base rate without differential?", "Is night shift guaranteed?", "Do weekend, overtime, and night rules stack?"),
                    "do not let conditional premium pay hide weak base compensation"),
            risk("orientation-length", "orientation length", "orientation",
                    "Short orientation can make a good rate unsafe.",
                    "Orientation length decides whether the unit is survivable, especially when acuity or specialty fit is uncertain.",
                    "Check whether the unit expects too much too soon.",
                    List.of("Orientation length is missing or too short.", "Competency progression is vague.", "Orientation can be shortened for staffing need."),
                    List.of("How many weeks are guaranteed?", "Can orientation extend if competency is not ready?", "What patients are assigned during onboarding?"),
                    "do not accept high acuity with vague onboarding"),
            risk("preceptor-support", "preceptor support", "preceptor support",
                    "The preceptor model decides whether orientation is real.",
                    "A dedicated preceptor, charge backup, and clear competency path reduce early failure risk.",
                    "Check who is responsible for helping you survive the unit.",
                    List.of("Preceptor is shared or not guaranteed.", "Charge backup is unclear.", "Competency signoff depends on staffing availability."),
                    List.of("Will I have a dedicated preceptor?", "Does charge carry patients while supporting new hires?", "How is competency signed off?"),
                    "do not rely on onboarding language without a support model"),
            risk("staffing-ratio", "staffing ratio", "staffing load",
                    "Ratio alone does not describe workload.",
                    "A ratio can look acceptable while acuity, turnover, and missing support make the shift unsustainable.",
                    "Read workload beyond the ratio.",
                    List.of("The offer mentions ratio without acuity.", "Admissions, discharges, or transfers are heavy.", "CNA, clerk, tech, or charge support is inconsistent."),
                    List.of("What is the typical assignment and acuity mix?", "How many admits/discharges per shift?", "What support exists on nights and weekends?"),
                    "do not accept a ratio answer as a workload answer"),
            risk("weekend-holiday-requirement", "weekend and holiday requirement", "schedule burden",
                    "Weekend and holiday burden changes the value of the offer.",
                    "Schedule burden can make a good rate hard to live with if frequency and premium pay are vague.",
                    "Check the schedule burden before accepting.",
                    List.of("Weekend frequency is not defined.", "Holiday rotation is vague.", "Premium pay does not match the burden."),
                    List.of("How many weekends per schedule period?", "How are holidays assigned?", "Can requirements change after hire?"),
                    "do not accept open-ended schedule burden"),
            risk("family-relocation-risk", "family relocation risk", "family relocation",
                    "A higher rate can still be the wrong version if the move splits the family.",
                    "Family separation changes retention risk, sleep, support, and whether the offer is livable after the first few weeks.",
                    "Check whether the relocation works as a real life plan, not just a pay raise.",
                    List.of("Family would stay behind while the nurse starts the role.", "The offer requires nights, rotation, or long weekends before support is in place.", "Relocation support does not cover the real cost of two households, travel, or transition time."),
                    List.of("What schedule would make the first 90 days workable for my family?", "Can start date, block scheduling, or weekend pattern be written down?", "What is the walk-away point if the family plan depends on hope?"),
                    "do not treat higher pay as enough"),
            risk("childcare-schedule-risk", "childcare and schedule risk", "childcare schedule",
                    "Nurse schedule risk is different when childcare is already tight.",
                    "Nights, weekends, call, and rotating shifts can make a strong hourly rate unusable if coverage fails at home.",
                    "Check whether the schedule can survive real childcare constraints.",
                    List.of("Night shift or rotating shift is not written clearly.", "Weekend, holiday, or call burden collides with childcare coverage.", "The offer assumes schedule flexibility the nurse does not actually have."),
                    List.of("Is the shift fixed, rotating, or manager-discretion after start?", "Can self-scheduling, block scheduling, or weekend pattern be confirmed?", "What happens if orientation or preceptor schedule conflicts with childcare?"),
                    "do not sign until the weekly coverage plan is real"),
            risk("commute-burnout-risk", "commute burnout risk", "commute burden",
                    "A commute can turn a good offer into a bad weekly life.",
                    "Commute time, parking, late shifts, and fatigue matter because nursing work already has high recovery cost.",
                    "Check whether the job is livable shift after shift.",
                    List.of("The commute is long after 12-hour shifts.", "Parking, traffic, or call response time is not accounted for.", "The offer only works if every handoff and commute goes perfectly."),
                    List.of("How long is the real commute at shift-change time?", "Can shift pattern, call, or start time be adjusted?", "Does the pay still work after commute time and recovery cost?"),
                    "do not accept a schedule that cannot be repeated safely"),
            risk("toxic-unit-culture", "toxic unit culture", "unit culture",
                    "Culture risk is part of the offer, not gossip outside the offer.",
                    "Bullying, high turnover, weak escalation, and unsafe assignments can erase the value of a strong rate.",
                    "Check whether the unit is survivable before accepting.",
                    List.of("Turnover, bullying, incivility, or unsafe assignments keep coming up.", "Manager support and escalation path are vague.", "The unit reputation is poor but the offer tries to compensate with money."),
                    List.of("What is recent turnover on this unit?", "How are unsafe assignments or incivility escalated?", "What support exists on nights, weekends, and high-acuity shifts?"),
                    "do not price culture risk as a minor inconvenience"));

    public static final List<String> INDEXABLE_PATHS = indexablePaths();
    private static final Map<String, IssuePage> PAGES = buildPages();

    private final ComparisonService comparisonService;

    public NurseOfferIssueController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    public static IssueContext issueContextFor(String rawIssue) {
        if (rawIssue == null || rawIssue.isBlank()) {
            return null;
        }
        String path = rawIssue.startsWith("/") ? rawIssue : "/" + rawIssue;
        IssuePage page = PAGES.get(path);
        if (page == null) {
            return null;
        }
        return new IssueContext(path.substring(1), page.h1(), page.issueLabel(), page.cardSummary(),
                page.questions(), page.walkAwayLine());
    }

    @GetMapping("/{slug:^(?:icu|ed|med-surg-tele|labor-delivery|or|pacu|nicu|picu|psych|oncology|dialysis|home-health)-rn-.+}")
    public String generatedIssuePage(@PathVariable String slug, Model model) {
        return renderIssuePage("/" + slug, model);
    }

    @GetMapping({
            "/rn-offer-red-flags",
            "/should-i-accept-nurse-job-offer",
            "/should-i-sign-nurse-job-offer",
            "/nurse-job-offer-review",
            "/nurse-offer-letter-red-flags",
            "/nurse-offer-before-signing-checklist",
            "/nurse-offer-life-fit",
            "/nurse-offer-family-relocation",
            "/nurse-night-shift-childcare-offer",
            "/nurse-offer-toxic-unit-culture",
            "/nurse-offer-negotiation-questions",
            "/compare-two-nurse-job-offers",
            "/rn-sign-on-bonus-clawback",
            "/rn-bonus-repayment-clause",
            "/rn-relocation-stipend-tax",
            "/nurse-relocation-package-worth-it",
            "/rn-guaranteed-hours-offer",
            "/rn-low-census-cancellation",
            "/rn-shift-guarantee-offer",
            "/rn-float-policy-offer",
            "/rn-float-pool-offer-red-flags",
            "/rn-pay-range-offer-letter",
            "/rn-night-shift-differential-offer",
            "/rn-weekend-holiday-requirement",
            "/rn-call-requirement-offer",
            "/rn-benefits-health-insurance-offer",
            "/rn-orientation-length-offer",
            "/rn-preceptor-support-offer",
            "/rn-staffing-ratio-offer",
            "/rn-unit-culture-red-flags",
            "/new-grad-nurse-offer-red-flags",
            "/travel-nurse-contract-red-flags",
            "/rn-prn-offer-red-flags",
            "/rn-internal-transfer-offer",
            "/icu-nurse-offer-red-flags",
            "/ed-nurse-offer-red-flags",
            "/med-surg-tele-nurse-offer-red-flags",
            "/labor-delivery-nurse-offer-red-flags"
    })
    public String issuePage(jakarta.servlet.http.HttpServletRequest request, Model model) {
        return renderIssuePage(request.getRequestURI(), model);
    }

    private String renderIssuePage(String path, Model model) {
        IssuePage page = PAGES.get(path);
        if (page == null) {
            throw new ResourceNotFoundException("RN offer issue page not found: " + path);
        }
        model.addAttribute("page", page);
        model.addAttribute("title", page.title());
        model.addAttribute("metaDescription", page.metaDescription());
        model.addAttribute("canonicalUrl", comparisonService.buildCanonicalUrl(path));
        model.addAttribute("shouldIndex", true);
        model.addAttribute("issuePath", path.substring(1));
        model.addAttribute("relatedPages", relatedPages(path));
        return "nurse-offer-issue";
    }

    private static List<String> indexablePaths() {
        List<String> paths = new ArrayList<>(SEED_PATHS);
        for (UnitIntent unit : UNIT_INTENTS) {
            for (RiskIntent risk : RISK_INTENTS) {
                paths.add(generatedPath(unit, risk));
            }
        }
        return List.copyOf(paths);
    }

    private static Map<String, IssuePage> buildPages() {
        Map<String, IssuePage> pages = new LinkedHashMap<>();
        for (String path : SEED_PATHS) {
            pages.put(path, pageFor(path));
        }
        for (UnitIntent unit : UNIT_INTENTS) {
            for (RiskIntent risk : RISK_INTENTS) {
                pages.put(generatedPath(unit, risk), generatedPage(unit, risk));
            }
        }
        return Map.copyOf(pages);
    }

    private static String generatedPath(UnitIntent unit, RiskIntent risk) {
        return "/" + unit.slug() + "-rn-" + risk.slug();
    }

    private static IssuePage generatedPage(UnitIntent unit, RiskIntent risk) {
        String unitTitle = unit.title();
        String riskTitle = risk.title();
        String h1 = unitTitle + " RN " + riskTitle;
        String title = h1 + " Review | OfferVerdict";
        String metaDescription = "Review " + h1.toLowerCase()
                + " before signing. Check written terms, red flags, questions to ask, and whether the RN offer is survivable.";
        String lead = "Use this when a " + unitTitle + " RN offer looks attractive but the "
                + riskTitle + " language could change whether you sign, negotiate, or walk away.";
        List<String> redFlags = new ArrayList<>(risk.redFlags());
        redFlags.add("The offer does not explain how this applies to " + unitTitle + " pressure: " + unit.pressure() + ".");
        List<String> questions = new ArrayList<>(risk.questions());
        questions.add(unit.question());
        return issue(
                title,
                metaDescription,
                h1,
                risk.subhead() + " For " + unitTitle + " roles, " + unit.pressure() + " can change the decision.",
                lead,
                unitTitle.toLowerCase() + " " + risk.issueLabel(),
                risk.whyItMatters() + " In " + unitTitle + ", " + unit.walkAwayContext() + ".",
                h1,
                risk.cardSummary(),
                List.copyOf(redFlags),
                List.copyOf(questions),
                "If the employer will not clarify " + riskTitle + " in writing, " + risk.walkAwayAction()
                        + ". For " + unitTitle + ", " + unit.walkAwayContext() + ".");
    }

    private static List<PageLink> relatedPages(String currentPath) {
        List<String> related = new ArrayList<>();
        UnitIntent currentUnit = unitForPath(currentPath);
        RiskIntent currentRisk = riskForPath(currentPath);

        if (currentUnit != null) {
            UNIT_INTENTS.stream()
                    .filter(unit -> unit.slug().equals(currentUnit.slug()))
                    .flatMap(unit -> RISK_INTENTS.stream().map(risk -> generatedPath(unit, risk)))
                    .filter(path -> !path.equals(currentPath))
                    .limit(4)
                    .forEach(related::add);
        }

        if (currentRisk != null) {
            UNIT_INTENTS.stream()
                    .map(unit -> generatedPath(unit, currentRisk))
                    .filter(path -> !path.equals(currentPath))
                    .filter(path -> !related.contains(path))
                    .limit(4)
                    .forEach(related::add);
        }

        List.of("/rn-offer-red-flags", "/nurse-job-offer-review", "/nurse-offer-negotiation-questions",
                "/nurse-offer-before-signing-checklist")
                .stream()
                .filter(path -> !path.equals(currentPath))
                .filter(path -> !related.contains(path))
                .forEach(related::add);

        INDEXABLE_PATHS.stream()
                .filter(path -> !path.equals(currentPath))
                .filter(path -> !related.contains(path))
                .limit(12)
                .forEach(related::add);

        return related.stream()
                .limit(12)
                .map(path -> {
                    IssuePage page = PAGES.get(path);
                    return new PageLink(path, page.issueLabel(), page.h1(), page.cardSummary());
                })
                .toList();
    }

    private static UnitIntent unitForPath(String path) {
        return UNIT_INTENTS.stream()
                .filter(unit -> path.startsWith("/" + unit.slug() + "-rn-"))
                .findFirst()
                .orElse(null);
    }

    private static RiskIntent riskForPath(String path) {
        UnitIntent unit = unitForPath(path);
        if (unit == null) {
            return null;
        }
        String riskSlug = path.substring(("/" + unit.slug() + "-rn-").length());
        return RISK_INTENTS.stream()
                .filter(risk -> risk.slug().equals(riskSlug))
                .findFirst()
                .orElse(null);
    }

    private static IssuePage pageFor(String path) {
        return switch (path) {
            case "/rn-offer-red-flags" -> issue(
                    "RN Offer Red Flags Before Signing | OfferVerdict",
                    "Check RN offer red flags before signing. Review pay certainty, clawbacks, relocation, float, cancellation, orientation, and unit survivability.",
                    "RN offer red flags before signing",
                    "The dangerous part is usually the clause you did not price.",
                    "Use this when an RN offer looks good on pay but still feels wrong. The point is to catch the terms that change the decision before you sign.",
                    "offer decision",
                    "A nurse offer can be financially attractive and still be a bad job if downside, schedule control, or unit support is vague.",
                    "Full RN offer red-flag review",
                    "Check the core risks before the package becomes binding.",
                    List.of("Pay depends on assumptions not written in the offer.", "Bonus or relocation money has repayment exposure.", "Unit support, float, cancellation, or orientation is vague."),
                    List.of("What terms are guaranteed in the final letter?", "Which downside clauses survive if the unit, shift, or employer changes?", "What would make this offer a negotiate or walk-away decision?"),
                    "If the written offer cannot prove the money is real and the unit is survivable, do not sign on headline pay.");
            case "/should-i-accept-nurse-job-offer" -> issue(
                    "Should I Accept This Nurse Job Offer? | OfferVerdict",
                    "Decide whether to accept, negotiate, or walk away from an RN job offer. Review pay, relocation, clawbacks, schedule risk, and unit fit.",
                    "Should I accept this nurse job offer?",
                    "A real offer needs a verdict, not another generic calculator.",
                    "This page is for the nurse who has an offer in hand and needs to know whether the written terms justify saying yes.",
                    "accept or negotiate",
                    "The accept decision should depend on guaranteed cash, contract downside, schedule control, and whether the unit is survivable.",
                    "Accept, negotiate, or walk away",
                    "Turn hesitation into a concrete next step.",
                    List.of("The offer feels good only because of bonus math.", "You are assuming shift, float, or hours are protected without written proof.", "The unit expectations are not clear enough for the risk."),
                    List.of("What would make this a yes if written into the offer?", "What clause would make you regret signing?", "Can you afford to leave if the unit is not survivable?"),
                    "If you cannot explain why this is a yes without relying on verbal promises, treat it as a negotiate decision.");
            case "/should-i-sign-nurse-job-offer" -> issue(
                    "Should I Sign This Nurse Job Offer? | OfferVerdict",
                    "Before signing a nurse job offer, pressure-test pay, clawbacks, schedule, family fit, unit culture, and whether the RN role is survivable.",
                    "Should I sign this nurse job offer?",
                    "This is for the last hesitation before signature.",
                    "Use this when the offer is real, the deadline is close, and one unresolved concern could turn a yes into negotiate or walk away.",
                    "signing hesitation",
                    "The signing decision should include money, downside, unit reality, and the personal constraint that made you hesitate in the first place.",
                    "Pre-sign RN offer decision",
                    "Turn the hesitation into a verdict and the next message to send.",
                    List.of("The offer pays more but would disrupt family, sleep, commute, or support.", "The written terms are clear on your obligations but vague on employer protections.", "The unit feels risky enough that money alone does not answer the decision."),
                    List.of("What exact concern would make me regret signing?", "Can that concern be solved in writing before start date?", "If nothing changes, would I still take this role after a bad first month?"),
                    "If the part making you hesitate cannot be answered or protected, do not sign just because the number improved.");
            case "/nurse-offer-life-fit" -> issue(
                    "Nurse Offer Life Fit Review | OfferVerdict",
                    "Review whether a nurse offer fits real life before signing. Check family distance, childcare, commute, nights, unit culture, and support.",
                    "Nurse offer life fit review",
                    "A better hourly rate can still be the wrong weekly life.",
                    "Use this when the math looks acceptable but the move, schedule, family setup, commute, or support system makes the decision feel unstable.",
                    "life fit",
                    "Life fit is not a soft preference when it decides whether the job can be repeated for months without breaking the nurse.",
                    "Life-fit offer review",
                    "Check whether the offer works outside the spreadsheet.",
                    List.of("Family, partner, or childcare constraints are not solved.", "Night, weekend, commute, or call burden collides with real life.", "The current team or support network is stronger than the new offer can prove."),
                    List.of("What weekly plan makes this offer livable?", "Which schedule terms need to be written down?", "What support am I giving up by accepting?"),
                    "If the life plan only works on hope, do not sign for the higher rate.");
            case "/nurse-offer-family-relocation" -> issue(
                    "Nurse Offer Family Relocation Risk | OfferVerdict",
                    "Check nurse offer family relocation risk before signing. Review pay, housing, family separation, start date, schedule, and relocation support.",
                    "Nurse offer family relocation risk",
                    "The move has to work for the household, not just the offer letter.",
                    "Use this when an RN offer requires relocation, the pay is tempting, but family distance or two-household pressure could change the decision.",
                    "family relocation",
                    "Family separation can make a strong offer fail because the real cost is travel, support loss, childcare, sleep, and retention risk.",
                    "Family relocation offer review",
                    "Decide whether the move is a real plan or just a pay raise.",
                    List.of("Family would stay behind during the transition.", "Relocation money does not cover the real cost of moving or two households.", "Nights, weekends, or orientation make the family plan fragile."),
                    List.of("What start date or schedule would make the transition workable?", "Can relocation, block scheduling, or weekend pattern be written down?", "What is the maximum time the family can realistically live split?"),
                    "If the family plan breaks before the contract risk ends, the offer is not safe to sign.");
            case "/nurse-night-shift-childcare-offer" -> issue(
                    "Nurse Night Shift Childcare Offer Risk | OfferVerdict",
                    "Review night shift and childcare risk in a nurse offer. Check fixed shifts, weekends, call, orientation schedule, and family coverage before signing.",
                    "Nurse night shift childcare offer risk",
                    "Night shift pay is not useful if the weekly coverage plan fails.",
                    "Use this when night shift, weekends, call, or orientation could collide with childcare even though the compensation looks better.",
                    "childcare schedule",
                    "Childcare turns schedule language into a hard decision constraint, not a minor preference.",
                    "Night shift childcare review",
                    "Check whether the schedule works at home before accepting.",
                    List.of("Night shift is not written as fixed.", "Weekend, holiday, call, or orientation schedule is unclear.", "Childcare coverage depends on a partner or family member absorbing unstable shifts."),
                    List.of("Is the shift fixed in the offer letter?", "What weekend, holiday, call, and orientation schedule applies?", "Can self-scheduling or block scheduling be confirmed before start?"),
                    "If childcare depends on schedule promises that are not written, negotiate before signing.");
            case "/nurse-offer-toxic-unit-culture" -> issue(
                    "Nurse Offer Toxic Unit Culture Risk | OfferVerdict",
                    "Check toxic unit culture risk before accepting a nurse offer. Review bullying, turnover, unsafe assignments, manager support, and escalation path.",
                    "Nurse offer toxic unit culture risk",
                    "Culture risk belongs inside the offer decision.",
                    "Use this when the offer looks attractive but you have heard about bullying, high turnover, unsafe assignments, or weak management support.",
                    "unit culture",
                    "A toxic unit can turn a good rate into a short stay with emotional, career, and repayment downside.",
                    "Toxic unit offer review",
                    "Pressure-test culture before it owns your schedule.",
                    List.of("Turnover or reputation concerns keep coming up.", "The manager cannot explain support or escalation clearly.", "The offer uses pay or bonus money to compensate for a unit people leave."),
                    List.of("What is recent turnover on this unit?", "How are unsafe assignments, bullying, or incivility handled?", "Who supports new nurses on nights and weekends?"),
                    "If the unit cannot prove support under pressure, do not let money silence the culture signal.");
            case "/nurse-job-offer-review" -> issue(
                    "Nurse Job Offer Review Tool | OfferVerdict",
                    "Review a nurse job offer before signing. Paste the offer letter to check compensation, relocation gaps, repayment clauses, and unit risk.",
                    "Nurse job offer review",
                    "Review the package like a decision, not a brochure.",
                    "Paste the written package and pressure-test the terms that actually change whether the offer is safe to accept.",
                    "offer review",
                    "A strong review separates what is guaranteed from what only works if every assumption goes right.",
                    "Document-first RN offer review",
                    "Use the letter, not recruiter memory, as the source of truth.",
                    List.of("Important terms are spread across recruiter email, job post, and offer letter.", "The hourly rate is clear but downside clauses are not.", "You are missing one or two terms that could change the verdict."),
                    List.of("Which terms are written, not verbal?", "What is still missing from the final package?", "What question should be sent back before signing?"),
                    "If the offer cannot survive a document-first review, it is not ready for signature.");
            case "/nurse-offer-letter-red-flags" -> issue(
                    "Nurse Offer Letter Red Flags | OfferVerdict",
                    "Find nurse offer letter red flags before signing. Check missing guarantees, vague unit language, repayment terms, and shift promises.",
                    "Nurse offer letter red flags",
                    "The offer letter should reduce uncertainty, not hide it.",
                    "Use this when the letter looks official but still leaves the real job, money, or downside unclear.",
                    "letter risk",
                    "The written letter is where verbal promises either become real or disappear.",
                    "Offer-letter red flags",
                    "Catch missing guarantees before they become your problem.",
                    List.of("The letter names pay but not guaranteed hours.", "Unit, shift, float, or cancellation language is vague.", "Repayment obligations are clearer than employer protections."),
                    List.of("What exactly is guaranteed in this letter?", "Which verbal promises are missing?", "Which clause creates the biggest downside if the job changes?"),
                    "If the letter is precise about your obligations but vague about employer commitments, pause before signing.");
            case "/nurse-offer-before-signing-checklist" -> issue(
                    "Nurse Offer Before Signing Checklist | OfferVerdict",
                    "Use this RN offer checklist before signing. Confirm pay, hours, shift, float, cancellation, bonuses, relocation, orientation, and unit support.",
                    "Nurse offer checklist before signing",
                    "Do not sign until the risky terms are visible.",
                    "This checklist focuses only on the terms that can change the decision after a nurse offer is already real.",
                    "signing checklist",
                    "A signing checklist should find decision-changing risk, not create a generic HR paperwork list.",
                    "Before-signing RN offer checklist",
                    "The checklist for the last review pass.",
                    List.of("You have not confirmed repayment triggers.", "Guaranteed hours and shift protection are missing.", "Orientation and support are assumed instead of written."),
                    List.of("What money is guaranteed versus conditional?", "What happens if hours, unit, or shift changes?", "Who supports you when the unit is unsafe or understaffed?"),
                    "If a missing answer could change your decision, get it in writing before you sign.");
            case "/nurse-offer-negotiation-questions" -> issue(
                    "Nurse Offer Negotiation Questions | OfferVerdict",
                    "Use RN offer negotiation questions before signing. Ask about pay, guaranteed hours, float, cancellation, relocation, clawbacks, and unit support.",
                    "Nurse offer negotiation questions",
                    "The best negotiation starts with the risk, not just the rate.",
                    "Use these questions when you need to send a concise message that improves the offer or exposes a hidden problem.",
                    "negotiation",
                    "A nurse offer negotiation should turn vague terms into written commitments.",
                    "Negotiation questions for RN offers",
                    "Ask the questions that change the package.",
                    List.of("You only ask for more hourly pay while ignoring downside clauses.", "The employer can answer verbally without changing the final letter.", "You do not know which ask matters most."),
                    List.of("Can guaranteed hours be written into the offer?", "Can repayment be prorated and limited to voluntary resignation?", "Can float scope and orientation be listed clearly?"),
                    "If they improve pay but refuse to clarify downside, the negotiation has not solved the real risk.");
            case "/compare-two-nurse-job-offers" -> issue(
                    "Compare Two Nurse Job Offers | OfferVerdict",
                    "Compare two nurse job offers before choosing. Review after-tax pay, relocation, bonuses, shift risk, float policy, and unit survivability.",
                    "Compare two nurse job offers",
                    "The higher rate is not always the better offer.",
                    "Use this when two RN offers look close and you need to compare guaranteed money, downside, and unit risk side by side.",
                    "offer comparison",
                    "Two offers can be close on pay and far apart on risk once clauses, hours, and unit support are priced.",
                    "RN offer comparison",
                    "Compare offers on regret risk, not just rate.",
                    List.of("One offer has better pay but worse repayment exposure.", "One unit is safer but the compensation gap feels tempting.", "Relocation or benefits change the real monthly outcome."),
                    List.of("Which offer has more guaranteed cash?", "Which offer is easier to leave if it goes wrong?", "Which unit has clearer support and orientation?"),
                    "If the higher-paying offer only wins under optimistic assumptions, do not treat it as the better offer yet.");
            case "/rn-bonus-repayment-clause" -> issue(
                    "RN Bonus Repayment Clause Review | OfferVerdict",
                    "Review an RN bonus repayment clause before signing. Check prorating, repayment triggers, termination language, and commitment length.",
                    "RN bonus repayment clause",
                    "A bonus clause can turn upside into debt.",
                    "Use this when the bonus looks attractive but the repayment language decides whether it is safe.",
                    "repayment clause",
                    "The clause matters more than the bonus amount because it defines who carries the downside if the job fails.",
                    "Bonus repayment clause review",
                    "Read the downside before counting the upside.",
                    List.of("Repayment is full instead of prorated.", "Repayment applies to termination without cause.", "Unit or schedule changes can trigger repayment."),
                    List.of("Is repayment forgiven monthly?", "Does repayment apply if the employer ends the role?", "Does repayment apply if the unit, shift, or location changes?"),
                    "If repayment is broad and not prorated, the bonus is leverage against you.");
            case "/rn-sign-on-bonus-clawback" -> issue(
                    "RN Sign-On Bonus Clawback Review | OfferVerdict",
                    "Review RN sign-on bonus clawback language before you sign. Check repayment triggers, prorating, termination clauses, and whether the bonus is worth the downside.",
                    "RN sign-on bonus clawback",
                    "Do not let a bonus hide the downside.",
                    "A sign-on bonus is not real upside until the repayment language is survivable. Paste the offer letter and check whether the clawback changes the decision.",
                    "repayment risk",
                    "A $10k or $20k bonus can look generous while the contract quietly puts the exit risk on you.",
                    "Sign-on bonus clawback",
                    "Check whether bonus money becomes a repayment trap.",
                    List.of("The repayment clause is full instead of prorated.", "Repayment triggers even if the employer ends the role or changes the unit.", "The commitment period is longer than the time you realistically expect to stay."),
                    List.of("Is repayment forgiven monthly or due in full?", "Does repayment apply if termination is without cause?", "Does a transfer, schedule change, or unit change trigger repayment?"),
                    "If they will not write proration and termination protection into the final offer, treat the bonus as risk, not income.");
            case "/rn-relocation-stipend-tax" -> issue(
                    "RN Relocation Stipend After Tax Review | OfferVerdict",
                    "Check whether an RN relocation stipend covers the move after taxes, timing, and repayment exposure. Review relocation gaps before signing.",
                    "RN relocation stipend after tax",
                    "Relocation money is not the same as move coverage.",
                    "A relocation stipend can be taxable, delayed, or repayable. The question is whether the move still works after the cash gap is real.",
                    "move economics",
                    "Relocation support should reduce risk, not create a larger locked-in downside.",
                    "Relocation stipend after tax",
                    "See whether relocation support still works after taxes and timing.",
                    List.of("The stipend is taxable but the offer compares it like cash.", "The real move cost is higher than the net stipend.", "Relocation repayment is tied to a long commitment period."),
                    List.of("When is relocation paid?", "Is the stipend taxable or paid through a vendor?", "Is relocation repayment prorated and separate from sign-on repayment?"),
                    "If the after-tax stipend does not cover the move and repayment is still attached, the relocation package is not protecting you.");
            case "/nurse-relocation-package-worth-it" -> issue(
                    "Is This Nurse Relocation Package Worth It? | OfferVerdict",
                    "Check whether a nurse relocation package is worth it after tax, rent, moving costs, repayment clauses, and unit risk.",
                    "Is this nurse relocation package worth it?",
                    "Relocation only works if the downside is survivable.",
                    "Use this when moving for an RN offer creates cash pressure, repayment exposure, or uncertainty about whether the job is worth the disruption.",
                    "relocation decision",
                    "A relocation package can help with the move while making it harder to leave if the unit is bad.",
                    "Relocation package worth-it check",
                    "Price the move, the taxes, and the lock-in.",
                    List.of("Relocation support is paid late or taxed heavily.", "The move cost exceeds the net support.", "Relocation repayment locks you into an uncertain unit."),
                    List.of("What is the net relocation amount after taxes?", "When is the money paid?", "Can repayment be prorated and separated from sign-on?"),
                    "If the package does not cover the move and still locks you in, relocation is not real protection.");
            case "/rn-guaranteed-hours-offer" -> issue(
                    "RN Guaranteed Hours Offer Review | OfferVerdict",
                    "Review RN guaranteed hours language before signing. Check FTE, scheduled hours, low census, cancellation, and pay certainty.",
                    "RN guaranteed hours in an offer",
                    "Guaranteed hours decide whether the pay is real.",
                    "Use this when the rate is attractive but the offer does not clearly protect scheduled hours.",
                    "hours guarantee",
                    "Hourly pay is only useful if the hours are protected enough to reach the income you are counting on.",
                    "Guaranteed hours review",
                    "Confirm the hours before trusting the rate.",
                    List.of("FTE is listed but weekly hours are unclear.", "Low census can reduce pay without protection.", "Bonus or repayment obligations remain even if hours disappear."),
                    List.of("Are weekly hours guaranteed in writing?", "What happens during low census?", "Does cancellation affect bonus or relocation repayment?"),
                    "If hours are not guaranteed but repayment is, the risk is asymmetric.");
            case "/rn-low-census-cancellation" -> issue(
                    "RN Low Census Cancellation Review | OfferVerdict",
                    "Review RN low census cancellation and guaranteed hours language before signing an offer. Check whether pay can disappear after relocation.",
                    "RN low census cancellation",
                    "Guaranteed hours matter more than the headline rate.",
                    "A strong rate can still fail if low census, flex-down, or cancellation language lets scheduled hours disappear after you relocate.",
                    "cancellation risk",
                    "Cancellation language decides whether the pay is reachable, especially after a move.",
                    "Low census cancellation review",
                    "Check whether scheduled hours can disappear after you sign.",
                    List.of("The offer can cancel shifts without pay.", "Guaranteed hours are verbal but missing from the letter.", "Low census policy shifts facility risk onto the nurse."),
                    List.of("Are scheduled hours guaranteed in writing?", "What happens if the unit cancels for low census?", "Does cancellation affect bonus or relocation repayment?"),
                    "If hours are not protected and repayment still applies, the offer puts too much facility risk on you.");
            case "/rn-shift-guarantee-offer" -> issue(
                    "RN Shift Guarantee Offer Review | OfferVerdict",
                    "Review RN shift guarantee language before signing. Check day/night shift, rotation, weekends, holidays, and whether the schedule is written.",
                    "RN shift guarantee in an offer",
                    "A verbal shift promise is not schedule control.",
                    "Use this when the recruiter promised days, nights, block scheduling, or stability but the final letter is less clear.",
                    "shift control",
                    "The shift changes commute, sleep, family life, differential pay, and whether the job is sustainable.",
                    "Shift guarantee review",
                    "Make the schedule real before signing.",
                    List.of("Shift is described verbally but not written.", "The offer allows rotation without limits.", "Differentials make the pay look better than base rate."),
                    List.of("Is the shift guaranteed in the offer letter?", "Can the employer rotate shifts after start?", "Are weekends, holidays, and call requirements defined?"),
                    "If the schedule matters to your decision, do not accept a vague shift promise.");
            case "/rn-float-policy-offer" -> issue(
                    "RN Float Policy Offer Review | OfferVerdict",
                    "Check RN offer float policy language before you sign. Review home unit, adjacent unit, hospital-wide float, campus float, and assignment risk.",
                    "RN float policy in an offer letter",
                    "A wide float radius can change the whole offer.",
                    "The same hourly rate means something different if you are protected on a home unit versus floated wherever staffing needs land.",
                    "float risk",
                    "Float language often sounds harmless until it decides the real job you are accepting.",
                    "Float policy review",
                    "Review whether the home unit is actually protected.",
                    List.of("The offer says hospital-wide float without naming units or competencies.", "The float scope crosses specialty boundaries without orientation support.", "The pay premium does not compensate for assignment uncertainty."),
                    List.of("What is the exact home unit?", "Which units and campuses can I float to?", "What orientation is provided for each float area?"),
                    "If the float radius is broad and they will not name the units in writing, do not treat the offer as a home-unit offer.");
            case "/rn-float-pool-offer-red-flags" -> issue(
                    "RN Float Pool Offer Red Flags | OfferVerdict",
                    "Review RN float pool offer red flags before signing. Check float radius, competencies, premium pay, orientation, cancellation, and unit support.",
                    "RN float pool offer red flags",
                    "Float pool pay has to compensate for real uncertainty.",
                    "Use this when the offer is explicitly float pool or resource nurse and the premium needs to be judged against scope and support.",
                    "float pool",
                    "Float pool can be a good deal only if the scope, support, and premium are explicit enough.",
                    "Float pool red flags",
                    "Check whether premium pay actually covers the uncertainty.",
                    List.of("Float scope crosses specialties without written competencies.", "Premium pay is small compared with assignment uncertainty.", "Orientation differs by unit but is not described."),
                    List.of("Which units and campuses are included?", "What assignments are excluded by competency?", "How much premium is paid for float pool risk?"),
                    "If float pool scope is broad but premium and orientation are vague, the offer is underpriced.");
            case "/rn-pay-range-offer-letter" -> issue(
                    "RN Pay Range In Offer Letter | OfferVerdict",
                    "Review RN pay range language in an offer letter. Check whether the actual rate, differential, FTE, and guaranteed hours are written clearly.",
                    "RN pay range in an offer letter",
                    "A posted range is not an actual offer.",
                    "Use this when a job post or recruiter mentions a range but the final rate and pay assumptions are not locked.",
                    "pay certainty",
                    "Pay range language can hide where your real offer sits and whether differentials are being used to make it look stronger.",
                    "Pay range review",
                    "Separate posted range from actual guaranteed rate.",
                    List.of("The offer repeats a range instead of naming your rate.", "The headline includes differentials that are not guaranteed.", "FTE and weekly hours are not tied to the rate."),
                    List.of("What is the exact base hourly rate?", "Which differentials are guaranteed?", "What weekly hours are attached to this rate?"),
                    "If the employer will not write the actual rate and hours, the pay is not locked.");
            case "/rn-night-shift-differential-offer" -> issue(
                    "RN Night Shift Differential Offer Review | OfferVerdict",
                    "Review RN night shift differential language before signing. Check base rate, differential rules, weekend stack, overtime, and schedule guarantee.",
                    "RN night shift differential in an offer",
                    "Night differential is upside only if the shift is real and livable.",
                    "Use this when the offer looks strong because of nights, weekends, or stacked differentials.",
                    "differential pay",
                    "Differentials can make weak base pay look better, but they depend on schedule and eligibility rules.",
                    "Night shift differential review",
                    "Separate real base pay from conditional upside.",
                    List.of("The base rate is weak without night differential.", "Differential eligibility is not written.", "The schedule may rotate, reducing expected pay."),
                    List.of("What is the base rate without differentials?", "Is night shift guaranteed?", "Do weekend and overtime rules stack with night differential?"),
                    "If the offer only works with perfect differential assumptions, negotiate the base or get the schedule written.");
            case "/rn-weekend-holiday-requirement" -> issue(
                    "RN Weekend And Holiday Requirement Review | OfferVerdict",
                    "Review RN weekend and holiday requirements before signing. Check frequency, rotation, premium pay, call, and schedule control.",
                    "RN weekend and holiday requirements",
                    "Weekend and holiday burden changes the value of the offer.",
                    "Use this when the schedule looks acceptable but the weekend or holiday requirement is vague.",
                    "schedule burden",
                    "A few words about weekends can decide whether the job fits your life.",
                    "Weekend and holiday review",
                    "Check the schedule burden before accepting.",
                    List.of("Weekend frequency is not stated.", "Holiday rotation is vague.", "Premium pay does not match the burden."),
                    List.of("How many weekends per schedule period?", "How are holidays assigned?", "Can requirements change after hire?"),
                    "If the schedule burden is open-ended, the offer is not fully priced.");
            case "/rn-call-requirement-offer" -> issue(
                    "RN Call Requirement Offer Review | OfferVerdict",
                    "Review RN call requirement language before signing. Check call frequency, callback pay, response time, weekends, holidays, and specialty impact.",
                    "RN call requirement in an offer",
                    "Call can quietly change the entire job.",
                    "Use this when the offer mentions call, standby, callback, or response time without enough detail.",
                    "call burden",
                    "Call affects sleep, family life, commute radius, and whether the hourly rate actually compensates the role.",
                    "Call requirement review",
                    "Price the hidden schedule burden.",
                    List.of("Call frequency is not defined.", "Callback pay or standby pay is missing.", "Response time makes the commute unrealistic."),
                    List.of("How often is call required?", "What is standby and callback pay?", "What response time is required?"),
                    "If call is required but pay and frequency are vague, the offer is incomplete.");
            case "/rn-benefits-health-insurance-offer" -> issue(
                    "RN Benefits And Health Insurance Offer Review | OfferVerdict",
                    "Review RN benefits and health insurance before accepting an offer. Check premiums, deductible, family coverage, retirement, PTO, and start date.",
                    "RN benefits and health insurance in an offer",
                    "Benefits can erase part of a raise.",
                    "Use this when the hourly rate improved but insurance, PTO, retirement, or start-date rules may change the real value.",
                    "benefits risk",
                    "Monthly cash flow and family coverage can change the decision even when hourly pay looks strong.",
                    "Benefits review",
                    "Check what the raise looks like after benefits.",
                    List.of("Insurance premium is much higher than current coverage.", "Benefits start after a waiting period.", "PTO or retirement match is worse than expected."),
                    List.of("What is the monthly premium for your coverage tier?", "When do benefits start?", "What are PTO, retirement match, and deductible differences?"),
                    "If benefits erase the pay improvement, negotiate or reprice the offer before signing.");
            case "/rn-orientation-length-offer" -> issue(
                    "RN Orientation Length Offer Review | OfferVerdict",
                    "Review RN orientation length before signing. Check specialty readiness, preceptor support, competency expectations, and early assignment risk.",
                    "RN orientation length in an offer",
                    "Short orientation can make a good rate unsafe.",
                    "Use this when the unit is higher acuity, new to you, or the offer gives little detail on onboarding.",
                    "orientation",
                    "Orientation is not a perk; it determines whether you can safely perform the role.",
                    "Orientation length review",
                    "Check whether the unit expects too much too soon.",
                    List.of("Orientation length is missing or short.", "Competency progression is vague.", "You are changing specialty without enough support."),
                    List.of("How many weeks of orientation are guaranteed?", "Is orientation extended if competency is not ready?", "What patient types are assigned during onboarding?"),
                    "If the unit risk is high and orientation is vague, do not sign on pay alone.");
            case "/rn-preceptor-support-offer" -> issue(
                    "RN Preceptor Support Offer Review | OfferVerdict",
                    "Review RN preceptor support before accepting an offer. Check dedicated preceptor, shared orientation, charge backup, and competency progression.",
                    "RN preceptor support in an offer",
                    "The preceptor model can decide whether the unit is survivable.",
                    "Use this when the offer names orientation but not who actually supports you on shift.",
                    "preceptor support",
                    "A dedicated preceptor and clear escalation path reduce risk more than vague onboarding language.",
                    "Preceptor support review",
                    "Check who is responsible for helping you survive the unit.",
                    List.of("Preceptor is not dedicated.", "Charge backup is unclear.", "Orientation expectations vary by staffing need."),
                    List.of("Will I have a dedicated preceptor?", "Does charge carry patients while supporting new hires?", "How is competency signed off?"),
                    "If support depends on whoever is available, the orientation promise is weak.");
            case "/rn-staffing-ratio-offer" -> issue(
                    "RN Staffing Ratio Offer Review | OfferVerdict",
                    "Review RN staffing ratio claims before signing. Check acuity, admits, discharges, CNA support, charge coverage, and typical assignment load.",
                    "RN staffing ratio in an offer",
                    "Ratio alone does not describe workload.",
                    "Use this when staffing sounds acceptable but acuity, turnover, or support could change the real burden.",
                    "staffing load",
                    "A ratio can look safe while admits, discharges, total care, or missing support make the shift hard.",
                    "Staffing ratio review",
                    "Read workload beyond the ratio.",
                    List.of("The ratio is stated without acuity or support.", "Admits, discharges, and transfers are heavy.", "CNA, tech, clerk, or charge support is inconsistent."),
                    List.of("What is the typical assignment and acuity mix?", "How many admits/discharges per shift?", "What support exists on nights and weekends?"),
                    "If ratio is the only staffing answer, you do not have enough to judge the offer.");
            case "/rn-unit-culture-red-flags" -> issue(
                    "RN Unit Culture Red Flags Before Accepting | OfferVerdict",
                    "Check RN unit culture red flags before accepting an offer. Ask about turnover, manager support, unsafe assignments, violence, incivility, and escalation.",
                    "RN unit culture red flags",
                    "Culture shows up as risk after you sign.",
                    "Use this when the offer looks good but the unit reputation, turnover, or manager support is uncertain.",
                    "unit culture",
                    "A bad unit can turn a good package into a fast regret if escalation and support are weak.",
                    "Unit culture red flags",
                    "Ask about the environment before it owns your schedule.",
                    List.of("Turnover is high or avoided in conversation.", "Unsafe assignments have no clear escalation path.", "Manager support is vague."),
                    List.of("What is recent unit turnover?", "How are unsafe assignments escalated?", "How does leadership respond to violence or incivility?"),
                    "If they cannot describe support under pressure, treat the culture risk as part of the offer.");
            case "/new-grad-nurse-offer-red-flags" -> issue(
                    "New Grad Nurse Offer Red Flags | OfferVerdict",
                    "Review new grad nurse offer red flags before signing. Check residency support, orientation length, preceptor model, unit acuity, and repayment clauses.",
                    "New grad nurse offer red flags",
                    "The first offer should not buy your burnout.",
                    "Use this when a new grad RN offer includes bonus money, repayment, or a high-pressure unit with unclear support.",
                    "new grad",
                    "New grad risk is different because orientation and preceptor quality can matter more than rate.",
                    "New grad offer red flags",
                    "Check support before being locked in.",
                    List.of("A large bonus comes with a long commitment.", "Orientation or residency structure is vague.", "The unit acuity is high without clear preceptor support."),
                    List.of("How long is residency or orientation?", "Is the preceptor dedicated?", "What happens if the unit fit is not safe?"),
                    "If the offer locks you in faster than it supports you, do not treat the bonus as a win.");
            case "/travel-nurse-contract-red-flags" -> issue(
                    "Travel Nurse Contract Red Flags | OfferVerdict",
                    "Review travel nurse contract red flags before signing. Check guaranteed hours, cancellation, stipend rules, float, call, extension, and facility risk.",
                    "Travel nurse contract red flags",
                    "Travel money depends on contract protection.",
                    "Use this when a travel contract looks attractive but cancellation, guaranteed hours, stipend, or float language can change the deal.",
                    "travel contract",
                    "A travel assignment can collapse financially if hours, stipends, or cancellation terms are weak.",
                    "Travel contract red flags",
                    "Check whether the contract protects the assignment.",
                    List.of("Guaranteed hours are missing or weak.", "Facility cancellation language is broad.", "Float scope or stipend rules are unclear."),
                    List.of("What hours are guaranteed?", "How many shifts can the facility cancel?", "What happens to stipends if hours or assignment change?"),
                    "If the contract shifts facility risk to you, the quoted weekly number is not reliable.");
            case "/rn-prn-offer-red-flags" -> issue(
                    "PRN RN Offer Red Flags | OfferVerdict",
                    "Review PRN RN offer red flags before accepting. Check minimum shifts, cancellation, premium pay, weekend rules, benefits loss, and scheduling control.",
                    "PRN RN offer red flags",
                    "PRN flexibility is valuable only if the rules are clear.",
                    "Use this when a PRN offer has strong hourly pay but uncertain scheduling, cancellation, or minimum commitment.",
                    "prn offer",
                    "PRN can be useful or unstable depending on minimum shifts, cancellation, and whether premium pay offsets lost benefits.",
                    "PRN offer red flags",
                    "Check whether flexibility is real.",
                    List.of("Minimum shifts are higher than expected.", "Cancellation risk is high.", "Premium pay does not offset benefits loss."),
                    List.of("What minimum shifts are required?", "Can shifts be cancelled without pay?", "What weekend or holiday rules apply?"),
                    "If PRN rules are strict but protections are weak, the offer gives the employer flexibility, not you.");
            case "/rn-internal-transfer-offer" -> issue(
                    "RN Internal Transfer Offer Review | OfferVerdict",
                    "Review an RN internal transfer offer before accepting. Check pay change, unit risk, shift, seniority, benefits, bonus eligibility, and support.",
                    "RN internal transfer offer",
                    "An internal move still needs offer-level scrutiny.",
                    "Use this when you are moving units or specialties inside the same system and the change feels lower risk than it really is.",
                    "internal transfer",
                    "Internal transfers can change shift, support, seniority, and workload even when employer and benefits stay familiar.",
                    "Internal transfer review",
                    "Check the hidden changes inside the same employer.",
                    List.of("Pay changes are smaller than the workload change.", "Seniority, schedule, or support changes are unclear.", "Orientation is assumed because you are already internal."),
                    List.of("What changes in pay, shift, and seniority?", "What orientation applies to the new unit?", "Can you return if the fit is unsafe?"),
                    "If the new unit creates new risk, review it like a new offer.");
            case "/icu-nurse-offer-red-flags" -> issue(
                    "ICU Nurse Offer Red Flags | OfferVerdict",
                    "Review ICU nurse offer red flags before signing. Check orientation, acuity, preceptor support, charge coverage, float risk, and clawbacks.",
                    "ICU nurse offer red flags",
                    "ICU pay only works if the support matches the acuity.",
                    "For ICU roles, the offer is not just rate and shift. Orientation, preceptor support, charge backup, and float boundaries decide whether the job is survivable.",
                    "icu survivability",
                    "High-acuity expectations without support can turn a good rate into a bad decision.",
                    "ICU offer red flags",
                    "Pressure-test acuity, orientation, charge support, and float.",
                    List.of("Orientation is short or not described.", "Charge carries an assignment and backup is unclear.", "Float includes stepdown or non-ICU units without clear protection."),
                    List.of("How long is ICU orientation?", "Is the preceptor dedicated?", "Does charge take patients, and when can ICU float outside critical care?"),
                    "If high-acuity expectations are clear but orientation and backup are vague, do not sign on pay alone.");
            case "/ed-nurse-offer-red-flags" -> issue(
                    "ED Nurse Offer Red Flags | OfferVerdict",
                    "Review emergency department nurse offer red flags before signing. Check boarding, psych burden, violence response, triage load, and shift terms.",
                    "ED nurse offer red flags",
                    "ED offers fail when chaos is hidden behind pay.",
                    "Emergency department offers need a different read because boarding, psych holds, hallway care, security, and triage load can dominate the actual job.",
                    "ed survivability",
                    "A high rate does not offset an unsafe or unsupported emergency department.",
                    "ED offer red flags",
                    "Check boarding, psych burden, security, and throughput risk.",
                    List.of("Boarding or hallway care is heavy but not discussed.", "Security and violence response are vague.", "Shift, float, or cancellation terms are loose."),
                    List.of("How often does the ED board admitted or psych patients?", "What security coverage is on shift?", "How are triage, fast-track, and high-acuity assignments split?"),
                    "If the ED cannot explain boarding, security, and staffing support clearly, downgrade the offer before negotiating rate.");
            case "/med-surg-tele-nurse-offer-red-flags" -> issue(
                    "Med-Surg Tele Nurse Offer Red Flags | OfferVerdict",
                    "Review med-surg and telemetry nurse offer red flags before signing. Check total-care load, admits, discharges, tele burden, CNA support, and float terms.",
                    "Med-surg tele nurse offer red flags",
                    "The real risk is workload, not just ratio.",
                    "Med-surg and tele roles can look ordinary on paper while admissions, discharges, telemetry load, and weak support make the shift much harder than the rate suggests.",
                    "med-surg / tele",
                    "A decent hourly rate can fail if the assignment is mostly total care with high turnover.",
                    "Med-surg tele offer red flags",
                    "Check workload, turnover, telemetry burden, and support.",
                    List.of("CNA or tech support is weak or not stated.", "Observation, overflow, or stepdown patients are mixed into the assignment.", "Admits, discharges, and transfers are high but not reflected in pay or staffing."),
                    List.of("What is the typical patient count and turnover per shift?", "Is CNA or tech support consistent on nights and weekends?", "Do observation, overflow, or stepdown patients land on this unit?"),
                    "If support is vague and turnover is high, do not compare this offer on hourly rate alone.");
            case "/labor-delivery-nurse-offer-red-flags" -> issue(
                    "Labor and Delivery Nurse Offer Red Flags | OfferVerdict",
                    "Review labor and delivery nurse offer red flags before signing. Check induction load, OB triage, C-section coverage, PACU expectations, and fetal monitoring support.",
                    "Labor and delivery nurse offer red flags",
                    "L&D risk is hidden in coverage and support.",
                    "Labor and delivery offers need a unit-specific read because induction, triage, C-section, PACU, hemorrhage, and fetal monitoring expectations can change the job fast.",
                    "l&d survivability",
                    "The offer should explain the support structure, not just the shift and rate.",
                    "L&D offer red flags",
                    "Review induction, triage, OR/PACU, and fetal monitoring support.",
                    List.of("Induction or augmentation load is not described.", "OB triage, C-section, or PACU coverage is unclear.", "Neonatal support and fetal monitoring expectations are vague."),
                    List.of("What are induction and triage expectations on this unit?", "Do L&D nurses cover C-section recovery or PACU?", "What fetal monitoring and neonatal support is available?"),
                    "If coverage expectations are broad but support is vague, the offer is not ready to sign.");
            default -> throw new IllegalArgumentException("Unknown RN offer issue path: " + path);
        };
    }

    private static IssuePage issue(String title,
                                   String metaDescription,
                                   String h1,
                                   String subhead,
                                   String lead,
                                   String issueLabel,
                                   String whyItMatters,
                                   String cardTitle,
                                   String cardSummary,
                                   List<String> redFlags,
                                   List<String> questions,
                                   String walkAwayLine) {
        String plainTopic = h1.replace("?", "").toLowerCase();
        String normalizedTopic = h1.replace("?", "");
        List<String> decisionChangers = List.of(
                "The final written offer confirms " + plainTopic + " clearly enough to price the downside.",
                "The term affects guaranteed pay, repayment exposure, schedule control, or whether the unit is survivable.",
                "The employer is willing to put the answer in the letter, not just explain it verbally.");
        List<String> strongerLanguage = List.of(
                "The offer names the exact term, timing, eligibility, and exceptions instead of relying on policy references.",
                "The clause protects the nurse if the employer changes unit, shift, location, hours, or role after acceptance.",
                "The final letter makes the expected pay and downside visible before start date.");
        List<String> weakLanguage = List.of(
                "Terms are described as subject to staffing needs, facility policy, or manager discretion without limits.",
                "Your obligation is written clearly but the employer commitment is vague.",
                "The offer depends on a recruiter promise that does not appear in the final document.");
        List<String> negotiationAsks = List.of(
                "Please add the " + normalizedTopic + " terms to the final written offer so I can review the package accurately.",
                "Can you confirm the trigger, limit, timing, and exception language in writing before I sign?",
                "If this term cannot be changed, can the compensation or commitment period be adjusted to reflect the risk?");
        List<String> toolInputs = List.of(
                "Offer letter or recruiter email",
                "Job post if the written offer is incomplete",
                "Current city, offer city, hourly rate, weekly hours, bonus, relocation, and contract length");
        return new IssuePage(title, metaDescription, h1, subhead, lead, issueLabel, whyItMatters, cardTitle,
                cardSummary, redFlags, questions, walkAwayLine, decisionChangers, strongerLanguage, weakLanguage,
                negotiationAsks, toolInputs);
    }

    private static UnitIntent unit(String slug,
                                   String title,
                                   String pressure,
                                   String question,
                                   String walkAwayContext) {
        return new UnitIntent(slug, title, pressure, question, walkAwayContext);
    }

    private static RiskIntent risk(String slug,
                                   String title,
                                   String issueLabel,
                                   String subhead,
                                   String whyItMatters,
                                   String cardSummary,
                                   List<String> redFlags,
                                   List<String> questions,
                                   String walkAwayAction) {
        return new RiskIntent(slug, title, issueLabel, subhead, whyItMatters, cardSummary, redFlags, questions,
                walkAwayAction);
    }

    private record UnitIntent(String slug,
                              String title,
                              String pressure,
                              String question,
                              String walkAwayContext) {
    }

    private record RiskIntent(String slug,
                              String title,
                              String issueLabel,
                              String subhead,
                              String whyItMatters,
                              String cardSummary,
                              List<String> redFlags,
                              List<String> questions,
                              String walkAwayAction) {
    }

    public record IssuePage(String title,
                            String metaDescription,
                            String h1,
                            String subhead,
                            String lead,
                            String issueLabel,
                            String whyItMatters,
                            String cardTitle,
                            String cardSummary,
                            List<String> redFlags,
                            List<String> questions,
                            String walkAwayLine,
                            List<String> decisionChangers,
                            List<String> strongerLanguage,
                            List<String> weakLanguage,
                            List<String> negotiationAsks,
                            List<String> toolInputs) {
    }

    public record PageLink(String path, String issueLabel, String title, String summary) {
    }

    public record IssueContext(String slug,
                               String title,
                               String label,
                               String summary,
                               List<String> questions,
                               String walkAwayLine) {
    }
}
