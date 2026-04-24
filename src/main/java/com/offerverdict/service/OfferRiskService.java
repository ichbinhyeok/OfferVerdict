package com.offerverdict.service;

import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.ComparisonBreakdown;
import com.offerverdict.model.HouseholdType;
import com.offerverdict.model.HousingType;
import com.offerverdict.model.JobInfo;
import com.offerverdict.model.OfferRiskReport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OfferRiskService {
    private static final double BONUS_TAX_RATE = 0.30;
    private static final double DEFAULT_401K_RATE_PERCENT = 5.0;
    private static final double DEFAULT_MONTHLY_INSURANCE = 150.0;

    private final DataRepository repository;
    private final SingleCityAnalysisService analysisService;

    public OfferRiskService(DataRepository repository, SingleCityAnalysisService analysisService) {
        this.repository = repository;
        this.analysisService = analysisService;
    }

    public OfferRiskReport assess(String analysisMode,
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
            String repaymentStyle) {
        return assess(analysisMode, roleSlug, currentCitySlug, offerCitySlug, unitType, shiftGuarantee, floatRisk,
                cancelRisk, currentHourlyRate, offerHourlyRate, weeklyHours, overtimeHours, nightDiffPercent,
                nightHours, weekendDiffPercent, weekendHours, currentMonthlyInsurance, offerMonthlyInsurance,
                signOnBonus, relocationStipend, movingCost, contractMonths, plannedStayMonths, repaymentStyle, "");
    }

    public OfferRiskReport assess(String analysisMode,
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
        String normalizedMode = normalizedAnalysisMode(analysisMode);

        CityCostEntry currentCity = repository.getCity(currentCitySlug);
        CityCostEntry offerCity = repository.getCity(offerCitySlug);
        JobInfo role = repository.findJobLoosely(roleSlug).orElseGet(() -> {
            JobInfo fallback = new JobInfo();
            fallback.setSlug("registered-nurse");
            fallback.setTitle("Healthcare worker");
            fallback.setCategory("Healthcare");
            return fallback;
        });

        double safeWeeklyHours = clamp(weeklyHours, 1, 80);
        double safeOvertimeHours = clamp(overtimeHours, 0, 40);
        double safeNightHours = clamp(nightHours, 0, safeWeeklyHours + safeOvertimeHours);
        double safeWeekendHours = clamp(weekendHours, 0, safeWeeklyHours + safeOvertimeHours);
        int safeContractMonths = Math.max(0, contractMonths);
        int safePlannedStayMonths = Math.max(0, plannedStayMonths);
        double safeCurrentMonthlyInsurance = Math.max(0, currentMonthlyInsurance);
        double safeOfferMonthlyInsurance = Math.max(0, offerMonthlyInsurance);

        double currentAnnualPay = annualBasePay(currentHourlyRate, safeWeeklyHours);
        double baseAnnualPay = annualBasePay(offerHourlyRate, safeWeeklyHours);
        double overtimeAnnualPay = Math.max(0, offerHourlyRate) * 1.5 * safeOvertimeHours * 52.0;
        double differentialAnnualPay = Math.max(0, offerHourlyRate)
                * ((Math.max(0, nightDiffPercent) / 100.0) * safeNightHours
                        + (Math.max(0, weekendDiffPercent) / 100.0) * safeWeekendHours)
                * 52.0;
        double offerAnnualPay = baseAnnualPay + overtimeAnnualPay + differentialAnnualPay;

        ComparisonBreakdown currentBreakdown = analyzeAnnualPay(currentAnnualPay, currentCity,
                safeCurrentMonthlyInsurance);
        ComparisonBreakdown offerBreakdown = analyzeAnnualPay(offerAnnualPay, offerCity,
                safeOfferMonthlyInsurance);

        double netUpfrontValue = (Math.max(0, signOnBonus) + Math.max(0, relocationStipend)) * (1.0 - BONUS_TAX_RATE)
                - Math.max(0, movingCost);
        double relocationGap = Math.max(0, Math.max(0, movingCost)
                - (Math.max(0, relocationStipend) * (1.0 - BONUS_TAX_RATE)));
        double repaymentExposure = repaymentExposure(signOnBonus, relocationStipend, safeContractMonths,
                safePlannedStayMonths, repaymentStyle);
        double monthlyDelta = offerBreakdown.getResidual() - currentBreakdown.getResidual();
        double breakEvenMonths = breakEvenMonths(netUpfrontValue, repaymentExposure, monthlyDelta);

        OfferRiskReport report = new OfferRiskReport();
        report.setAnalysisMode(normalizedMode);
        report.setAnalysisModeLabel("job_post".equals(normalizedMode) ? "Job Post Screen" : "Offer Review");
        report.setRoleLabel(role.getTitle());
        report.setCurrentCityName(currentCity.getCity() + ", " + currentCity.getState());
        report.setOfferCityName(offerCity.getCity() + ", " + offerCity.getState());
        report.setCurrentAnnualPay(currentAnnualPay);
        report.setOfferAnnualPay(offerAnnualPay);
        report.setBaseAnnualPay(baseAnnualPay);
        report.setOvertimeAnnualPay(overtimeAnnualPay);
        report.setDifferentialAnnualPay(differentialAnnualPay);
        report.setCurrentMonthlyResidual(currentBreakdown.getResidual());
        report.setOfferMonthlyResidual(offerBreakdown.getResidual());
        report.setMonthlyResidualDelta(monthlyDelta);
        report.setCurrentMonthlyInsurance(safeCurrentMonthlyInsurance);
        report.setOfferMonthlyInsurance(safeOfferMonthlyInsurance);
        report.setMonthlyInsuranceDelta(safeOfferMonthlyInsurance - safeCurrentMonthlyInsurance);
        report.setSignOnBonus(Math.max(0, signOnBonus));
        report.setRelocationStipend(Math.max(0, relocationStipend));
        report.setMovingCost(Math.max(0, movingCost));
        report.setEstimatedBonusTaxRate(BONUS_TAX_RATE);
        report.setEstimatedNetUpfrontValue(netUpfrontValue);
        report.setRelocationCoverageGap(relocationGap);
        report.setContractMonths(safeContractMonths);
        report.setPlannedStayMonths(safePlannedStayMonths);
        report.setRepaymentStyle(normalizedRepaymentStyle(repaymentStyle));
        report.setRepaymentExposure(repaymentExposure);
        report.setBreakEvenMonths(breakEvenMonths);
        report.setUnitTypeLabel(unitTypeLabel(unitType));
        report.setShiftGuaranteeLabel(shiftGuaranteeLabel(shiftGuarantee));
        report.setFloatRiskLabel(floatRiskLabel(floatRisk));
        report.setCancelRiskLabel(cancelRiskLabel(cancelRisk));
        report.setNurseScheduleRiskScore(nurseScheduleRiskScore(shiftGuarantee, floatRisk, cancelRisk));
        LifeFitProfile lifeFitProfile = analyzeLifeFit(sourceText);
        report.setLifeFitRiskScore(lifeFitProfile.score());
        report.setLifeFitLabel(lifeFitProfile.label());
        report.setLifeFitSummary(lifeFitProfile.summary());
        report.setLifeFitSignals(lifeFitProfile.signals());
        applyMarketAnchor(report, role.getSlug(), offerCity.getSlug(), offerAnnualPay);
        applyVerdict(report);
        report.setRedFlags(redFlags(report));
        report.setHrQuestions(hrQuestions(report));
        report.setNegotiationMoves(negotiationMoves(report));
        applyDecisionSupport(report);
        return report;
    }

    private ComparisonBreakdown analyzeAnnualPay(double annualPay, CityCostEntry city, double monthlyInsurance) {
        return analysisService.analyze(
                Math.max(1_000, annualPay),
                city,
                repository.getAuthoritativeMetrics(),
                HouseholdType.SINGLE,
                HousingType.RENT,
                false,
                DEFAULT_401K_RATE_PERCENT,
                monthlyInsurance > 0 ? monthlyInsurance : DEFAULT_MONTHLY_INSURANCE,
                0.0,
                0.0,
                0.0,
                false,
                true,
                0.0,
                0.0,
                1.0,
                0.0);
    }

    private void applyMarketAnchor(OfferRiskReport report, String roleSlug, String offerCitySlug, double offerAnnualPay) {
        DataRepository.MarketBenchmarkSelection selection = repository.selectMarketBenchmark(roleSlug, offerCitySlug);
        Map<String, Double> benchmark = selection.values();
        double p50 = benchmark.getOrDefault("p50", 0.0);
        if (p50 <= 0) {
            report.setOfferPercentileAnchor(0.0);
            report.setMarketAnchorLabel("No reliable role anchor");
            return;
        }
        double position = ((offerAnnualPay / p50) * 100.0) - 100.0;
        report.setOfferPercentileAnchor(position);
        if (selection.citySpecific()) {
            report.setMarketAnchorLabel("Local wage anchor");
        } else if (selection.roleSpecific()) {
            report.setMarketAnchorLabel("Role wage anchor");
        } else {
            report.setMarketAnchorLabel("Broad wage anchor");
        }
    }

    private void applyVerdict(OfferRiskReport report) {
        if ("job_post".equals(report.getAnalysisMode())) {
            applyJobPostVerdict(report);
            return;
        }
        double monthlyDelta = report.getMonthlyResidualDelta();
        double repaymentExposure = report.getRepaymentExposure();
        double stayValue = Math.max(0, monthlyDelta) * Math.max(1, report.getPlannedStayMonths());

        if (report.getOfferMonthlyResidual() < 0 || repaymentExposure > stayValue) {
            report.setVerdict("WALK AWAY");
            report.setVerdictTone("danger");
            report.setVerdictSummary("The offer puts too much cash or repayment risk on you before the upside is earned.");
            return;
        }

        if (report.getNurseScheduleRiskScore() >= 5 && repaymentExposure > (stayValue * 0.6)) {
            report.setVerdict("WALK AWAY");
            report.setVerdictTone("danger");
            report.setVerdictSummary("The money depends on schedule terms that are too loose for the repayment risk.");
            return;
        }

        if (report.getLifeFitRiskScore() >= 7
                && (report.getNurseScheduleRiskScore() >= 5 || repaymentExposure > 0 || monthlyDelta < 500)) {
            report.setVerdict("WALK AWAY");
            report.setVerdictTone("danger");
            report.setVerdictSummary("The pay may improve, but this version pushes too much life, family, or unit risk onto you.");
            return;
        }

        if (report.getLifeFitRiskScore() >= 3) {
            report.setVerdict("NEGOTIATE");
            report.setVerdictTone("warning");
            report.setVerdictSummary("The money may work, but the life-fit constraints need a written plan before this is signable.");
            return;
        }

        if (monthlyDelta < 500 || repaymentExposure > 0 || report.getRelocationCoverageGap() > 0
                || report.getBreakEvenMonths() > 6 || report.getNurseScheduleRiskScore() >= 2
                || report.getMonthlyInsuranceDelta() > 250) {
            report.setVerdict("NEGOTIATE");
            report.setVerdictTone("warning");
            report.setVerdictSummary("There may be upside, but the contract and relocation terms need better protection.");
            return;
        }

        report.setVerdict("ACCEPTABLE");
        report.setVerdictTone("success");
        report.setVerdictSummary("The monthly cash improvement appears strong enough to survive the basic relocation risks.");
    }

    private void applyJobPostVerdict(OfferRiskReport report) {
        double offerLeftover = report.getOfferMonthlyResidual();
        double anchor = report.getOfferPercentileAnchor();
        int scheduleRisk = report.getNurseScheduleRiskScore();

        if (offerLeftover < 0 || (anchor < -10 && scheduleRisk >= 4)) {
            report.setVerdict("LOW SIGNAL");
            report.setVerdictTone("danger");
            report.setVerdictSummary("The listing does not look strong enough on local cash or terms to prioritize.");
            return;
        }

        if (offerLeftover < 1500 || anchor < 0 || scheduleRisk >= 2
                || report.getContractMonths() > 0 || report.getMonthlyInsuranceDelta() > 250) {
            report.setVerdict("CHECK TERMS");
            report.setVerdictTone("warning");
            report.setVerdictSummary("The listing may be worth a recruiter call, but key terms still need verification.");
            return;
        }

        report.setVerdict("WORTH SCREENING");
        report.setVerdictTone("success");
        report.setVerdictSummary("The listing looks strong enough to pursue and verify in a recruiter screen.");
    }

    private LifeFitProfile analyzeLifeFit(String sourceText) {
        List<String> signals = new ArrayList<>();
        if (sourceText == null || sourceText.isBlank()) {
            return new LifeFitProfile(0, "No major life-fit signal detected",
                    "No personal constraint was stated, so this read stays focused on pay, terms, and unit risk.",
                    signals);
        }

        String text = sourceText.toLowerCase(Locale.ROOT);
        int score = 0;

        if (containsAny(text, "away from my family", "away from family", "far from family", "leave my family",
                "leaving my family", "family would stay", "family will stay", "family is staying",
                "live apart", "living apart", "separate from family", "separated from family",
                "without my family", "two homes", "long distance", "partner stays", "spouse stays")) {
            score += 4;
            signals.add("Family separation is a decision constraint, not a small lifestyle preference.");
        }
        if (containsAny(text, "away from my kids", "leave my kids", "leaving my kids", "kids stay",
                "kids", "childcare", "child care", "daycare", "school pickup", "kid pickup", "kids' school",
                "my children", "single parent")) {
            score += 3;
            signals.add("Childcare or kid-schedule pressure can make a higher hourly rate fail in real life.");
        }
        if (containsAny(text, "partner's job", "partner job", "spouse job", "spouse's job", "partner schedule",
                "spouse schedule", "my partner", "my spouse")) {
            score += 2;
            signals.add("Partner or spouse constraints need a real weekly plan before the offer is safe to sign.");
        }
        if (containsAny(text, "long commute", "commute", "traffic", "parking", "drive would be",
                "drive is", "one hour drive", "two hour drive")) {
            score += 1;
            signals.add("Commute friction can quietly erase the quality-of-life upside.");
        }
        if (containsAny(text, "coworkers are good", "coworkers are great", "good coworkers", "great coworkers",
                "supportive manager", "good manager", "team is good", "like my team", "love my team",
                "current team")) {
            score += 2;
            signals.add("Leaving a proven team is opportunity cost; the new unit must prove support, not just pay.");
        }
        if (containsAny(text, "toxic", "bullying", "hostile", "unsafe", "lateral violence", "nurses eat their young",
                "taeum", "mean girl", "cliquey", "dumpster fire", "bad culture", "no breaks", "short staffed",
                "understaffed", "unsafe ratio", "unsafe ratios", "burnout")) {
            score += 4;
            signals.add("Culture, staffing, or bullying risk changes survivability; it should not be treated as a minor clause.");
        }
        if (containsAny(text, "night shift", "nights", "rotating", "weekends", "every weekend")
                && containsAny(text, "family", "kids", "childcare", "daycare", "partner", "spouse", "school")) {
            score += 2;
            signals.add("Schedule strain plus family constraints is a hard-fit issue, not just shift differential math.");
        }

        if (signals.isEmpty()) {
            return new LifeFitProfile(0, "No major life-fit signal detected",
                    "No personal constraint was stated, so this read stays focused on pay, terms, and unit risk.",
                    signals);
        }
        if (score >= 7) {
            return new LifeFitProfile(score, "Life fit risk is high",
                    "The offer can pay more and still be the wrong version if family separation, schedule strain, or unit culture breaks the job.",
                    signals.stream().limit(4).toList());
        }
        if (score >= 3) {
            return new LifeFitProfile(score, "Life fit needs a plan",
                    "This offer needs a concrete life plan before it deserves a signature; the pay comparison is not enough.",
                    signals.stream().limit(4).toList());
        }
        return new LifeFitProfile(score, "Life fit signal detected",
                "A personal constraint showed up. It may not block the offer, but it belongs in the decision.",
                signals.stream().limit(4).toList());
    }

    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private List<String> redFlags(OfferRiskReport report) {
        List<String> flags = new ArrayList<>();
        if (report.getRepaymentExposure() > 0) {
            flags.add("You could owe about $" + dollars(report.getRepaymentExposure())
                    + " if you leave before the commitment period is complete.");
        }
        if ("job_post".equals(report.getAnalysisMode()) && report.getOfferMonthlyResidual() < 0) {
            flags.add("The posted pay may leave very little monthly cash after taxes, rent, and baseline local costs.");
        }
        if ("offer_review".equals(report.getAnalysisMode()) && report.getMonthlyResidualDelta() < 0) {
            flags.add("The offer city leaves less monthly cash after taxes, rent, and baseline living costs.");
        }
        if (report.getRelocationCoverageGap() > 0) {
            flags.add("Relocation support appears short by about $" + dollars(report.getRelocationCoverageGap())
                    + " after estimated taxes.");
        }
        if (report.getDifferentialAnnualPay() > report.getBaseAnnualPay() * 0.15) {
            flags.add("A large share of the offer depends on shift differentials. Confirm the schedule is guaranteed.");
        }
        if (report.getNurseScheduleRiskScore() >= 2) {
            flags.add("Nurse-specific schedule terms need review: " + report.getShiftGuaranteeLabel() + ", "
                    + report.getFloatRiskLabel() + ", " + report.getCancelRiskLabel() + ".");
        }
        if (report.getMonthlyInsuranceDelta() > 250) {
            flags.add("Offer health insurance is about $" + dollars(report.getMonthlyInsuranceDelta())
                    + "/mo higher than the current plan.");
        }
        if (flags.isEmpty()) {
            if ("job_post".equals(report.getAnalysisMode())) {
                flags.add("No major screen-out signal surfaced. Still confirm schedule, float, cancellation, and any lock-in terms with a recruiter.");
            } else {
                flags.add("No major model red flag surfaced. Still confirm repayment, schedule, and relocation language in writing.");
            }
        }
        return flags;
    }

    private List<String> hrQuestions(OfferRiskReport report) {
        List<String> questions = new ArrayList<>();
        if ("job_post".equals(report.getAnalysisMode())) {
            questions.add("Is the posted base rate guaranteed for this exact unit, shift, and facility?");
            questions.add("Are there contract lock-ins, bonus clawbacks, or relocation repayment terms not shown in the listing?");
            questions.add("Are night, weekend, call, and overtime differentials real, current, and written into the offer?");
        } else {
            questions.add("Is the sign-on and relocation repayment prorated, forgiven monthly, or due in full?");
            questions.add("Does repayment trigger if the employer ends the assignment, cancels shifts, or changes unit/facility?");
            questions.add("Are night, weekend, call, and overtime differentials guaranteed in the written offer?");
        }
        questions.add("What is the home unit, nurse-to-patient ratio, float radius, and orientation length?");
        questions.add("Are scheduled hours protected if census drops or the unit cancels shifts?");
        questions.add("Which relocation costs are reimbursed, which are taxable, and when are they paid?");
        return questions;
    }

    private List<String> negotiationMoves(OfferRiskReport report) {
        List<String> moves = new ArrayList<>();
        if ("job_post".equals(report.getAnalysisMode())) {
            moves.add("Send a recruiter message that asks for the exact unit, shift, float policy, and cancellation policy before you apply.");
            if (report.getOfferPercentileAnchor() < 0) {
                moves.add("Treat the posted pay as a weak signal unless the recruiter can justify why it sits below the local role anchor.");
            }
            if (report.getMonthlyInsuranceDelta() > 250) {
                moves.add("Ask for the actual employee premium sheet before assuming the posted pay is competitive.");
            }
            if (report.getContractMonths() > 0) {
                moves.add("Do not treat a listed bonus as real upside until repayment and lock-in terms are shown in writing.");
            }
            moves.add("If the recruiter answers vaguely on shift, float, or cancellations, downgrade the listing.");
            return moves;
        }

        if (report.getRepaymentExposure() > 0) {
            moves.add("Ask for monthly proration and no repayment if termination is without cause.");
        }
        if (report.getRelocationCoverageGap() > 0) {
            moves.add("Ask for an additional $" + dollars(report.getRelocationCoverageGap())
                    + " relocation gross-up or direct vendor payment.");
        }
        if (report.getMonthlyResidualDelta() < 500) {
            moves.add("Ask for a base-rate increase before accepting variable differential upside.");
        }
        if (report.getNurseScheduleRiskScore() >= 2) {
            moves.add("Ask to write the unit, shift, float radius, cancellation rules, and guaranteed hours into the offer.");
        }
        if (report.getMonthlyInsuranceDelta() > 250) {
            moves.add("Ask for a base-rate bump or stipend to offset the higher health insurance premium.");
        }
        moves.add("Get the accepted unit, shift, weekly hours, and differential schedule in the offer letter.");
        return moves;
    }

    private void applyDecisionSupport(OfferRiskReport report) {
        report.setVerdictReasons(verdictReasons(report));
        report.setDecisionLockLabel(decisionLockLabel(report));
        report.setDecisionLocks(decisionLocks(report));
        report.setSwingFactors(swingFactors(report));
        report.setActionLabel(actionLabel(report));
        report.setActionDraft(actionDraft(report));
        report.setConfidenceLabel(confidenceLabel(report));
        report.setConfidenceSummary(confidenceSummary(report));
        report.setPacketSummary(packetSummary(report));
        report.setTopRisks(topRisks(report));
        report.setSurvivabilityHeadline(survivabilityHeadline(report));
        report.setSurvivabilitySummary(survivabilitySummary(report));
        report.setSurvivabilitySignals(survivabilitySignals(report));
        report.setMustAskNow(mustAskNow(report));
        report.setWalkAwayLine(walkAwayLine(report));
    }

    private String packetSummary(OfferRiskReport report) {
        if ("job_post".equals(report.getAnalysisMode())) {
            if (report.getNurseScheduleRiskScore() >= 2 && report.getOfferPercentileAnchor() < 0) {
                return "This listing is weak on both pay quality and term clarity. It is not just one missing detail.";
            }
            if (report.getNurseScheduleRiskScore() >= 2) {
                return "This listing is mostly a term-clarity problem. It only deserves time if a recruiter can prove the shift, float, and cancellation terms.";
            }
            if (report.getOfferPercentileAnchor() < 0) {
                return "This listing is mostly a pay-quality problem. The post already looks light for the market before you verify the fine print.";
            }
            return "Use this like a screen, not a verdict. The real decision starts when the written offer arrives.";
        }

        if (report.getLifeFitRiskScore() >= 7) {
            return "This is mainly a life-fit and survivability problem. The offer cannot be judged by money until the family, schedule, or culture constraint has a real plan.";
        }
        if (report.getLifeFitRiskScore() >= 3) {
            return "This is not just a pay comparison. The offer needs a concrete life-fit plan before the numbers are signable.";
        }
        if (report.getRepaymentExposure() > 0 && report.getNurseScheduleRiskScore() >= 2) {
            return "This is mainly a downside-allocation problem. Too much risk sits on you before the offer is safe enough to sign.";
        }
        if (report.getMonthlyResidualDelta() < 500) {
            return "This is mainly a thin-upside problem. The move does not create enough real breathing room for the downside.";
        }
        if (report.getRelocationCoverageGap() > 0) {
            return "This is mainly an upfront-cash problem. The relocation package still leaves too much of the move on you.";
        }
        if (report.getNurseScheduleRiskScore() >= 2) {
            return "This is mainly a schedule-control problem. The upside still depends on terms that are too vague to trust.";
        }
        return "This packet looks closer to a real win than a trap, but only if the written unit and schedule stay exactly as reviewed.";
    }

    private List<String> topRisks(OfferRiskReport report) {
        List<String> risks = new ArrayList<>();

        if ("job_post".equals(report.getAnalysisMode())) {
            if (report.getOfferMonthlyResidual() < 0) {
                risks.add("The posted pay looks underwater after modeled tax, rent, and baseline local costs.");
            } else if (report.getOfferMonthlyResidual() < 1500) {
                risks.add("The listing only leaves about $" + dollars(report.getOfferMonthlyResidual())
                        + "/mo in modeled breathing room, which is thin for a move.");
            }
            if (report.getOfferPercentileAnchor() < 0) {
                risks.add("Posted pay sits about " + percent(Math.abs(report.getOfferPercentileAnchor()))
                        + " below the local wage anchor.");
            }
            if (report.getNurseScheduleRiskScore() >= 2) {
                risks.add("Shift, float, or cancellation language is still vague enough to hide a worse real job than the listing suggests.");
            }
            if (report.getContractMonths() > 0 || report.getSignOnBonus() > 0 || report.getRelocationStipend() > 0) {
                risks.add("The incentive package may hide a lock-in or clawback before you have written terms.");
            }
            if (report.getOfferMonthlyInsurance() > 250) {
                risks.add("The listed health premium could strip another $" + dollars(report.getOfferMonthlyInsurance())
                        + "/mo from take-home.");
            }
            if (risks.isEmpty()) {
                risks.add("The listing is usable, but the real screen still depends on the written unit, shift, and pay floor.");
            }
            return risks.stream().limit(3).toList();
        }

        if (report.getLifeFitRiskScore() >= 3) {
            risks.add(report.getLifeFitLabel() + ": " + report.getLifeFitSummary());
        }
        if (report.getMonthlyResidualDelta() < 0) {
            risks.add("The move leaves you about $" + dollars(Math.abs(report.getMonthlyResidualDelta()))
                    + "/mo worse off after modeled local costs.");
        } else if (report.getMonthlyResidualDelta() < 500) {
            risks.add("The move only adds about $" + dollars(report.getMonthlyResidualDelta())
                    + "/mo after modeled costs, which is thin for a relocation.");
        }
        if (report.getRepaymentExposure() > 0) {
            risks.add("You still carry about $" + dollars(report.getRepaymentExposure())
                    + " of clawback exposure if this job does not last.");
        }
        if (report.getNurseScheduleRiskScore() >= 2) {
            risks.add("The upside depends on shift, float, or cancellation terms that are still too loose to trust.");
        }
        if (report.getRelocationCoverageGap() > 0) {
            risks.add("Relocation support still looks short by about $" + dollars(report.getRelocationCoverageGap())
                    + " after estimated taxes.");
        }
        if (report.getMonthlyInsuranceDelta() > 250) {
            risks.add("The health plan appears to add about $" + dollars(report.getMonthlyInsuranceDelta())
                    + "/mo in premium cost.");
        }
        if (report.getBreakEvenMonths() > 6 && report.getBreakEvenMonths() < 999) {
            risks.add("The move takes about " + (int) Math.round(report.getBreakEvenMonths())
                    + " months to break even, which is slow for this level of risk.");
        }
        if (risks.isEmpty()) {
            risks.add("The main remaining risk is final-letter drift: the unit, shift, or guaranteed hours changing after verbal agreement.");
        }
        return risks.stream().limit(3).toList();
    }

    private String survivabilityHeadline(OfferRiskReport report) {
        if ("job_post".equals(report.getAnalysisMode())) {
            return "Can this " + report.getUnitTypeLabel() + " role actually be livable?";
        }
        return "Can you survive this " + report.getUnitTypeLabel() + " offer?";
    }

    private String survivabilitySummary(OfferRiskReport report) {
        String base;
        if (report.getUnitTypeLabel().contains("ICU")) {
            base = "ICU offers fail when high-acuity expectations outrun orientation, backup, and written schedule protection.";
        } else if (report.getUnitTypeLabel().contains("Emergency")) {
            base = "ED offers fail when boarding, violence exposure, and throughput chaos are hidden behind a decent rate.";
        } else if (report.getUnitTypeLabel().contains("Labor and delivery")) {
            base = "L&D offers fail when induction, triage, OR/PACU coverage, and fetal monitoring support are weaker than the pay makes them look.";
        } else if (report.getUnitTypeLabel().contains("Float")) {
            base = "Float roles fail when the pay premium is not high enough to justify wide assignment uncertainty.";
        } else if (report.getUnitTypeLabel().contains("Clinic")) {
            base = "Clinic offers usually fail on low upside, schedule rigidity, or benefits, not on dramatic shift differential math.";
        } else {
            base = "Med-surg and tele offers fail when total-care load, turnover, and weak support make the money harder to keep than it looks on paper.";
        }

        if (report.getNurseScheduleRiskScore() >= 5) {
            return base + " Right now the written control of shift, float, and cancellations is too weak to treat this as survivable.";
        }
        if (report.getLifeFitRiskScore() >= 7) {
            return base + " The stated family, schedule, or culture constraint makes survivability the main decision, not the hourly rate.";
        }
        if (report.getNurseScheduleRiskScore() >= 2) {
            return base + " Right now the shift and staffing terms still need tightening before you trust the offer.";
        }
        if ("job_post".equals(report.getAnalysisMode())) {
            return base + " The listing can only answer this partially until a written packet confirms the unit terms.";
        }
        return base + " The money may work, but unit survivability still depends on orientation, support, and whether the written terms stay clean.";
    }

    private List<String> survivabilitySignals(OfferRiskReport report) {
        List<String> signals = new ArrayList<>();

        if (report.getUnitTypeLabel().contains("ICU")) {
            signals.add("Confirm orientation length, preceptor model, and whether charge carries an assignment.");
            signals.add("Clarify whether ICU can float you outside adjacent critical-care support or step you down into unrelated coverage.");
            signals.add("Treat differential-heavy upside carefully if the real shift mix or protected hours are still soft.");
        } else if (report.getUnitTypeLabel().contains("Emergency")) {
            signals.add("Ask about boarding load, psych volume, security response, and whether charge carries patients.");
            signals.add("Clarify triage, hallway care, and how often high-acuity and fast-track work get mixed on one shift.");
            signals.add("If the listing is vague on shifts or cancellations, assume ED chaos can erase pay upside fast.");
        } else if (report.getUnitTypeLabel().contains("Labor and delivery")) {
            signals.add("Ask about induction ratios, OB triage load, C-section or PACU coverage, and hemorrhage backup.");
            signals.add("Clarify fetal monitoring expectations, neonatal support, and whether postpartum or triage overflow lands on the role.");
            signals.add("Do not trust the pay signal if unit support and schedule control are still vague.");
        } else if (report.getUnitTypeLabel().contains("Float")) {
            signals.add("Ask exactly which units, campuses, and skill levels sit inside the float radius.");
            signals.add("Clarify whether assignments are paired with orientation support or if you are expected to land fully ready everywhere.");
            signals.add("A float premium only matters if the premium is real and the assignment scope is written.");
        } else if (report.getUnitTypeLabel().contains("Clinic")) {
            signals.add("Ask about staffing ratios, late add-ons, phone triage, and whether the schedule really stays predictable.");
            signals.add("Clarify benefits and base pay because clinic offers often win or lose on steady take-home, not bonuses.");
            signals.add("If this role still hides schedule drift or unpaid expectations, the quality-of-life upside is weaker than it looks.");
        } else {
            signals.add("Ask about admits, discharges, transfers, tele burden, and whether CNA or tech support is dependable.");
            signals.add("Clarify whether observation, stepdown, or overflow patients land on the same assignment.");
            signals.add("If the unit is vague on float or low-census cancellations, the real workload can outgrow the rate quickly.");
        }
        if (report.getLifeFitRiskScore() >= 3) {
            signals.add("Do a real weekly-life check: commute, sleep, family coverage, orientation load, and support network after the move.");
        }

        return signals;
    }

    private List<String> mustAskNow(OfferRiskReport report) {
        List<String> questions = new ArrayList<>();

        if ("job_post".equals(report.getAnalysisMode())) {
            questions.add("What is the exact base rate for this unit, shift, and facility, not the broad posting range?");
            questions.add("What are the written float, cancellation, and guaranteed-hours rules for this role?");
        } else {
            questions.add("What exactly triggers repayment, and is termination without cause excluded from any clawback?");
            questions.add("Will the final offer write the unit, shift, float radius, cancellation rules, and guaranteed hours explicitly?");
            if (report.getLifeFitRiskScore() >= 3) {
                questions.add("What written schedule, self-scheduling, block scheduling, weekend, or orientation terms make the stated life constraint workable?");
            }
        }

        if (report.getUnitTypeLabel().contains("ICU")) {
            questions.add("How long is ICU orientation, how is precepting structured, and does charge take an assignment?");
        } else if (report.getUnitTypeLabel().contains("Emergency")) {
            questions.add("How often does this ED board psych or admitted patients, and what security coverage is on shift?");
        } else if (report.getUnitTypeLabel().contains("Labor and delivery")) {
            questions.add("What are the induction, triage, and C-section coverage expectations on this unit?");
        } else if (report.getUnitTypeLabel().contains("Float")) {
            questions.add("Exactly which units and campuses sit inside the float pool, and what orientation exists for each?");
        } else if (report.getUnitTypeLabel().contains("Clinic")) {
            questions.add("What does clinic staffing actually look like on a normal day, including triage and add-on load?");
        } else {
            questions.add("What are the admit-discharge-turnover expectations, tele burden, and CNA or tech coverage on a typical shift?");
        }

        return questions.stream().limit(report.getLifeFitRiskScore() >= 3 ? 4 : 3).toList();
    }

    private String walkAwayLine(OfferRiskReport report) {
        if ("job_post".equals(report.getAnalysisMode())) {
            if (report.getNurseScheduleRiskScore() >= 2
                    || report.getContractMonths() > 0
                    || report.getSignOnBonus() > 0
                    || report.getRelocationStipend() > 0) {
                return "If the recruiter will not send the real base rate, float policy, and cancellation language in writing, stop here.";
            }
            return "If the written offer lands below the posted pay floor or quietly changes the unit or shift, reopen the whole decision.";
        }

        if (report.getLifeFitRiskScore() >= 7) {
            return "If the family, schedule, or support plan only works on hope, do not sign for the higher hourly rate.";
        }
        if (report.getRepaymentExposure() > 0 || report.getContractMonths() > 0) {
            return "If they will not write the unit, shift, float radius, and clawback protections into the final letter, do not sign.";
        }
        if (report.getMonthlyResidualDelta() < 0) {
            return "If base pay does not move enough to create real monthly breathing room, do not relocate for this offer.";
        }
        return "If the final letter changes the unit, shift, or guaranteed hours from what you reviewed, pause instead of signing.";
    }

    private List<String> verdictReasons(OfferRiskReport report) {
        List<String> reasons = new ArrayList<>();

        if ("job_post".equals(report.getAnalysisMode())) {
            if (report.getOfferMonthlyResidual() < 0) {
                reasons.add("The posted pay looks underwater after tax, rent, and baseline local costs in "
                        + report.getOfferCityName() + ".");
            } else if (report.getOfferMonthlyResidual() < 1500) {
                reasons.add("The listing only leaves about $" + dollars(report.getOfferMonthlyResidual())
                        + "/mo after modeled local costs, which is thin for a move.");
            } else {
                reasons.add("The listing leaves roughly $" + dollars(report.getOfferMonthlyResidual())
                        + "/mo after modeled local costs, so the pay signal is usable.");
            }

            if (report.getOfferPercentileAnchor() < 0) {
                reasons.add("Posted pay sits about " + percent(Math.abs(report.getOfferPercentileAnchor()))
                        + " below the local wage anchor.");
            } else if (report.getOfferPercentileAnchor() > 0) {
                reasons.add("Posted pay sits about " + percent(report.getOfferPercentileAnchor())
                        + " above the local wage anchor.");
            }

            if (report.getNurseScheduleRiskScore() >= 2) {
                reasons.add("Shift, float, or cancellation language is still too vague to trust the listing at face value.");
            }
            if (report.getContractMonths() > 0) {
                reasons.add("The listing already signals a " + report.getContractMonths()
                        + "-month lock-in before you have written repayment terms.");
            }
            if (report.getOfferMonthlyInsurance() > 250) {
                reasons.add("The listed employee premium could strip another $" + dollars(report.getOfferMonthlyInsurance())
                        + "/mo from take-home.");
            }
            if (reasons.isEmpty()) {
                reasons.add("The listing does not show an obvious screen-out signal on pay or terms.");
            }
            return reasons;
        }

        if (report.getMonthlyResidualDelta() < 0) {
            reasons.add("This move reduces your leftover cash by about $" + dollars(Math.abs(report.getMonthlyResidualDelta()))
                    + "/mo after local costs.");
        } else if (report.getMonthlyResidualDelta() < 500) {
            reasons.add("This offer only improves leftover cash by about $" + dollars(report.getMonthlyResidualDelta())
                    + "/mo, which is thin for a relocation.");
        } else {
            reasons.add("This offer improves leftover cash by about $" + dollars(report.getMonthlyResidualDelta())
                    + "/mo after tax, rent, insurance, and baseline costs.");
        }

        if (report.getRepaymentExposure() > 0) {
            reasons.add("You still carry about $" + dollars(report.getRepaymentExposure())
                    + " of bonus or relocation clawback exposure.");
        }
        if (report.getRelocationCoverageGap() > 0) {
            reasons.add("Relocation support still looks short by about $" + dollars(report.getRelocationCoverageGap())
                    + " after estimated taxes.");
        }
        if (report.getNurseScheduleRiskScore() >= 2) {
            reasons.add("The upside depends on schedule terms that are not clean enough yet: "
                    + report.getShiftGuaranteeLabel() + ", " + report.getFloatRiskLabel() + ", "
                    + report.getCancelRiskLabel() + ".");
        }
        if (report.getMonthlyInsuranceDelta() > 250) {
            reasons.add("The offer health plan appears to add about $" + dollars(report.getMonthlyInsuranceDelta())
                    + "/mo in premium cost.");
        }
        if (report.getLifeFitRiskScore() >= 3) {
            reasons.add(report.getLifeFitSummary());
        }
        if (report.getBreakEvenMonths() >= 999) {
            reasons.add("Under the current assumptions, the move does not reach break-even.");
        } else if (report.getBreakEvenMonths() > 6) {
            reasons.add("The move takes about " + (int) Math.round(report.getBreakEvenMonths())
                    + " months to break even, which is slow for the risk.");
        }
        if (reasons.size() == 1 && "ACCEPTABLE".equals(report.getVerdict())) {
            reasons.add("Repayment exposure is low enough that the monthly gain is not being swallowed by clawback risk.");
            reasons.add("Schedule terms look clean enough that the modeled upside is closer to real pay.");
        }
        return reasons;
    }

    private String decisionLockLabel(OfferRiskReport report) {
        if ("job_post".equals(report.getAnalysisMode())) {
            return "Before you spend time";
        }
        if ("ACCEPTABLE".equals(report.getVerdict())) {
            return "Protect the win";
        }
        return "Do not sign until";
    }

    private List<String> decisionLocks(OfferRiskReport report) {
        List<String> locks = new ArrayList<>();

        if ("job_post".equals(report.getAnalysisMode())) {
            locks.add("Get the exact base rate for this unit, shift, and facility before you treat the listing as real.");
            if (report.getNurseScheduleRiskScore() >= 2) {
                locks.add("Get shift, float, and cancellation policy in writing or downgrade the listing.");
            }
            if (report.getContractMonths() > 0 || report.getSignOnBonus() > 0 || report.getRelocationStipend() > 0) {
                locks.add("Get bonus repayment and lock-in terms before you count incentives as upside.");
            }
            if (report.getOfferMonthlyInsurance() > 250) {
                locks.add("Get the employee premium sheet before comparing take-home pay.");
            }
            if (locks.size() < 3) {
                locks.add("If the recruiter stays vague, move on instead of investing more time.");
            }
            return locks;
        }

        if (report.getRepaymentExposure() > 0) {
            locks.add("Get monthly proration and no repayment if termination is without cause.");
        }
        if (report.getLifeFitRiskScore() >= 3) {
            locks.add("Write down the family, commute, childcare, or support plan that makes this job livable before treating the pay as real upside.");
        }
        if (report.getNurseScheduleRiskScore() >= 2 || report.getDifferentialAnnualPay() > report.getBaseAnnualPay() * 0.15) {
            locks.add("Get the unit, shift, float radius, cancellation rules, and guaranteed hours written into the offer.");
        }
        if (report.getRelocationCoverageGap() > 0) {
            locks.add("Close the relocation gap or do not assume the move pencils out.");
        }
        if (report.getMonthlyInsuranceDelta() > 250) {
            locks.add("Review the final benefit premium sheet before you compare take-home.");
        }
        if (report.getMonthlyResidualDelta() < 0 || report.getBreakEvenMonths() >= 999) {
            locks.add("Do not take the move unless base pay or guaranteed support changes materially.");
        }
        if (locks.isEmpty()) {
            locks.add("Make sure the final letter matches the verbal package on unit, shift, weekly hours, and pay.");
        }
        return locks;
    }

    private String actionLabel(OfferRiskReport report) {
        if ("job_post".equals(report.getAnalysisMode())) {
            return "Copy this recruiter message";
        }
        if ("ACCEPTABLE".equals(report.getVerdict())) {
            return "Copy this confirmation note";
        }
        return "Copy this negotiation note";
    }

    private String actionDraft(OfferRiskReport report) {
        List<String> asks = new ArrayList<>();

        if ("job_post".equals(report.getAnalysisMode())) {
            asks.add("the exact base rate for this unit, shift, and facility");
            if (report.getNurseScheduleRiskScore() >= 2) {
                asks.add("the written float policy, cancellation policy, and guaranteed hours");
            }
            if (report.getContractMonths() > 0 || report.getSignOnBonus() > 0 || report.getRelocationStipend() > 0) {
                asks.add("any lock-in, repayment, or sign-on clawback terms tied to the posting");
            }
            if (report.getOfferMonthlyInsurance() > 0) {
                asks.add("the employee premium sheet for the health plan");
            }

            StringBuilder draft = new StringBuilder();
            draft.append("Hi, I'm interested in the ")
                    .append(report.getRoleLabel())
                    .append(" opening in ")
                    .append(report.getOfferCityName())
                    .append(".\n\nBefore I spend more time, can you confirm:\n");
            for (String ask : asks) {
                draft.append("- ").append(ask).append("\n");
            }
            draft.append("\nIf those terms line up, I'm happy to take the next step.");
            return draft.toString();
        }

        if (report.getRepaymentExposure() > 0) {
            asks.add("monthly proration and no repayment if termination is without cause");
        }
        if (report.getRelocationCoverageGap() > 0) {
            asks.add("additional relocation support or a base-rate adjustment to close the remaining move gap");
        }
        if (report.getNurseScheduleRiskScore() >= 2 || report.getDifferentialAnnualPay() > report.getBaseAnnualPay() * 0.15) {
            asks.add("the final written unit, shift, float radius, cancellation language, and guaranteed hours");
        }
        if (report.getLifeFitRiskScore() >= 3) {
            asks.add("the schedule terms that make the stated family, commute, childcare, or support constraint workable");
        }
        if (report.getMonthlyInsuranceDelta() > 250) {
            asks.add("the final employee premium sheet and any stipend or rate adjustment tied to it");
        }
        if (asks.isEmpty()) {
            asks.add("the final written unit, shift, weekly hours, and differential schedule");
        }

        StringBuilder draft = new StringBuilder();
        draft.append("Hi [Recruiter/HR],\n\n");
        if ("ACCEPTABLE".equals(report.getVerdict())) {
            draft.append("Thanks again for the offer. I'm interested in moving forward, and before I sign I want to confirm:\n");
        } else {
            draft.append("Thanks for the offer. I'm interested, but I need these items tightened in writing before I can decide:\n");
        }
        for (String ask : asks) {
            draft.append("- ").append(ask).append("\n");
        }
        draft.append("\nIf we can get those terms confirmed, I'm open to moving quickly.");
        return draft.toString();
    }

    private String confidenceLabel(OfferRiskReport report) {
        int score = 0;
        if ("offer_review".equals(report.getAnalysisMode())) {
            score += 2;
        } else {
            score += 1;
        }
        if (!"Shift not confirmed".equals(report.getShiftGuaranteeLabel()) && !"Rotating shift".equals(report.getShiftGuaranteeLabel())) {
            score += 1;
        }
        if (!"Float terms unknown".equals(report.getFloatRiskLabel())) {
            score += 1;
        }
        if (!"Cancellation terms unknown".equals(report.getCancelRiskLabel())) {
            score += 1;
        }
        if (report.getSignOnBonus() + report.getRelocationStipend() <= 0 || report.getContractMonths() > 0) {
            score += 1;
        }
        if (report.getOfferPercentileAnchor() != 0) {
            score += 1;
        }

        if (score >= 6) {
            return "High confidence";
        }
        if (score >= 4) {
            return "Moderate confidence";
        }
        return "Low confidence";
    }

    private String confidenceSummary(OfferRiskReport report) {
        if ("High confidence".equals(report.getConfidenceLabel())) {
            return "Most of the terms that usually hide downside are explicit enough to trust this read.";
        }
        if ("Moderate confidence".equals(report.getConfidenceLabel())) {
            return "This is directionally useful, but a few terms can still swing the answer.";
        }
        return "Too many terms are still fuzzy to trust this result without follow-up.";
    }

    private List<String> swingFactors(OfferRiskReport report) {
        List<String> factors = new ArrayList<>();

        if ("job_post".equals(report.getAnalysisMode())) {
            if ("Shift not confirmed".equals(report.getShiftGuaranteeLabel())
                    || "Rotating shift".equals(report.getShiftGuaranteeLabel())) {
                factors.add("The answer changes fast if the real base rate only applies to a harder shift mix than the post implies.");
            }
            if ("Float terms unknown".equals(report.getFloatRiskLabel())
                    || "Cancellation terms unknown".equals(report.getCancelRiskLabel())) {
                factors.add("Hospital-wide float or weak cancellation language can erase a listing that otherwise looks decent on paper.");
            }
            if (report.getContractMonths() <= 0 && (report.getSignOnBonus() > 0 || report.getRelocationStipend() > 0)) {
                factors.add("Any hidden lock-in or clawback tied to the incentive package can turn posted upside into repayment risk.");
            }
            if (report.getOfferMonthlyInsurance() <= 0) {
                factors.add("Benefits are still a blind spot here. A high employee premium can materially compress take-home pay.");
            }
            if (factors.isEmpty()) {
                factors.add("This quick screen mostly flips only if the written offer shows a different shift, pay floor, or lock-in than the listing.");
            }
            return factors;
        }

        if ("Shift not confirmed".equals(report.getShiftGuaranteeLabel())
                || "Rotating shift".equals(report.getShiftGuaranteeLabel())) {
            factors.add("If the guaranteed shift mix changes, the modeled differential upside changes with it.");
        }
        if ("Float terms unknown".equals(report.getFloatRiskLabel())
                || "Cancellation terms unknown".equals(report.getCancelRiskLabel())) {
            factors.add("Float radius and cancellation protection still matter because they change whether this pay is actually reachable.");
        }
        if (report.getRepaymentExposure() > 0) {
            factors.add("Clawback language still matters. Full repayment or no monthly proration can flip a decent offer into a bad one.");
        }
        if (report.getRelocationCoverageGap() > 0) {
            factors.add("The move economics change if taxable relocation support ends up lower than expected or if your real move cost runs above estimate.");
        }
        if (report.getMonthlyInsuranceDelta() == 0 && report.getOfferMonthlyInsurance() <= 0) {
            factors.add("Final benefit premiums are still unverified, so true take-home can land lower than this draft shows.");
        }
        if (factors.isEmpty()) {
            factors.add("This read mostly changes only if the final written letter differs from the pay, shift, or unit already shown.");
        }
        return factors;
    }

    private String normalizedAnalysisMode(String analysisMode) {
        return "job_post".equalsIgnoreCase(analysisMode) ? "job_post" : "offer_review";
    }

    private String unitTypeLabel(String unitType) {
        if ("icu".equalsIgnoreCase(unitType)) {
            return "ICU / critical care";
        }
        if ("ed".equalsIgnoreCase(unitType)) {
            return "Emergency department";
        }
        if ("or".equalsIgnoreCase(unitType)) {
            return "Operating room";
        }
        if ("l_and_d".equalsIgnoreCase(unitType)) {
            return "Labor and delivery";
        }
        if ("clinic".equalsIgnoreCase(unitType)) {
            return "Clinic / outpatient";
        }
        if ("float_pool".equalsIgnoreCase(unitType)) {
            return "Float pool";
        }
        if ("other".equalsIgnoreCase(unitType)) {
            return "Other unit";
        }
        return "Med-surg / telemetry";
    }

    private String shiftGuaranteeLabel(String shiftGuarantee) {
        if ("verbal".equalsIgnoreCase(shiftGuarantee)) {
            return "Shift discussed but not written";
        }
        if ("rotating".equalsIgnoreCase(shiftGuarantee)) {
            return "Rotating shift";
        }
        if ("unknown".equalsIgnoreCase(shiftGuarantee)) {
            return "Shift not confirmed";
        }
        return "Written shift schedule";
    }

    private String floatRiskLabel(String floatRisk) {
        if ("adjacent_units".equalsIgnoreCase(floatRisk)) {
            return "Float to adjacent units";
        }
        if ("hospital_wide".equalsIgnoreCase(floatRisk)) {
            return "Hospital-wide float";
        }
        if ("unknown".equalsIgnoreCase(floatRisk)) {
            return "Float terms unknown";
        }
        return "Home unit only";
    }

    private String cancelRiskLabel(String cancelRisk) {
        if ("low_census_only".equalsIgnoreCase(cancelRisk)) {
            return "Low-census cancellation possible";
        }
        if ("can_cancel_without_pay".equalsIgnoreCase(cancelRisk)) {
            return "Can cancel without pay";
        }
        if ("unknown".equalsIgnoreCase(cancelRisk)) {
            return "Cancellation terms unknown";
        }
        return "Guaranteed scheduled hours";
    }

    private int nurseScheduleRiskScore(String shiftGuarantee, String floatRisk, String cancelRisk) {
        int score = 0;
        if ("verbal".equalsIgnoreCase(shiftGuarantee)) {
            score += 1;
        } else if ("rotating".equalsIgnoreCase(shiftGuarantee) || "unknown".equalsIgnoreCase(shiftGuarantee)) {
            score += 2;
        }
        if ("adjacent_units".equalsIgnoreCase(floatRisk)) {
            score += 1;
        } else if ("hospital_wide".equalsIgnoreCase(floatRisk) || "unknown".equalsIgnoreCase(floatRisk)) {
            score += 2;
        }
        if ("low_census_only".equalsIgnoreCase(cancelRisk)) {
            score += 1;
        } else if ("can_cancel_without_pay".equalsIgnoreCase(cancelRisk) || "unknown".equalsIgnoreCase(cancelRisk)) {
            score += 2;
        }
        return score;
    }

    private double repaymentExposure(double signOnBonus, double relocationStipend, int contractMonths,
            int plannedStayMonths, String repaymentStyle) {
        double total = Math.max(0, signOnBonus) + Math.max(0, relocationStipend);
        if (total <= 0 || contractMonths <= 0 || plannedStayMonths >= contractMonths) {
            return 0.0;
        }
        if ("none".equalsIgnoreCase(repaymentStyle)) {
            return 0.0;
        }
        if ("full".equalsIgnoreCase(repaymentStyle)) {
            return total;
        }
        double remainingShare = (double) (contractMonths - plannedStayMonths) / contractMonths;
        return total * remainingShare;
    }

    private double breakEvenMonths(double netUpfrontValue, double repaymentExposure, double monthlyDelta) {
        double atRiskAmount = Math.max(0, -netUpfrontValue) + Math.max(0, repaymentExposure);
        if (atRiskAmount <= 0 && monthlyDelta > 0) {
            return 0.0;
        }
        if (monthlyDelta <= 0) {
            return 999.0;
        }
        return Math.ceil(atRiskAmount / monthlyDelta);
    }

    private double annualBasePay(double hourlyRate, double weeklyHours) {
        return Math.max(0, hourlyRate) * Math.max(0, weeklyHours) * 52.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalizedRepaymentStyle(String repaymentStyle) {
        if ("full".equalsIgnoreCase(repaymentStyle)) {
            return "Full repayment";
        }
        if ("none".equalsIgnoreCase(repaymentStyle)) {
            return "No repayment";
        }
        return "Prorated repayment";
    }

    private String dollars(double value) {
        return String.format("%,.0f", Math.max(0, value));
    }

    private String percent(double value) {
        return String.format("%,.1f%%", Math.max(0, value));
    }

    private record LifeFitProfile(int score, String label, String summary, List<String> signals) {
    }
}
