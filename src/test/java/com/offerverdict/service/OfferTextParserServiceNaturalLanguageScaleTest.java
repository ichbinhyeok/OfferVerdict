package com.offerverdict.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.OfferRiskDraft;
import com.offerverdict.model.OfferTextParseResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferTextParserServiceNaturalLanguageScaleTest {

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesFiveHundredNaturalLanguageInputsWithoutCrossWiringCriticalFacts() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(completeOfferCases());
        cases.addAll(abbreviationCases());
        cases.addAll(lowInformationConcernCases());
        cases.addAll(jobPostCases());

        assertEquals(500, cases.size(), "test corpus size drifted");
        assertCasesPass(cases);
    }

    @Test
    void parsesFiveHundredMoreDiverseNaturalLanguageInputs() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(completeConcernCases());
        cases.addAll(messyStructuredNoteCases());
        cases.addAll(moneyFormattingCases());
        cases.addAll(decisionStageJobPostCases());
        cases.addAll(partialDecisionAnxietyCases());

        assertEquals(500, cases.size(), "diverse corpus size drifted");
        assertCasesPass(cases);
    }

    @Test
    void parsesFiveHundredAdversarialNaturalLanguageInputs() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(reversedOrderOfferCases());
        cases.addAll(symbolHeavyMobileNoteCases());
        cases.addAll(shiftPremiumAndBonusSeparationCases());
        cases.addAll(ambiguousMoneyButMissingBasicsCases());
        cases.addAll(jobPostNoiseCases());

        assertEquals(500, cases.size(), "adversarial corpus size drifted");
        assertCasesPass(cases);
    }

    @Test
    void parsesThreeThousandFiveHundredHumanLikeNaturalLanguageInputs() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(humanDoubtWithCompleteFactsCases());
        cases.addAll(recruiterThreadPasteCases());
        cases.addAll(jobPostPasteWithHumanQuestionCases());
        cases.addAll(lowInfoPersonalTradeoffCases());
        cases.addAll(moneyLabelConfusionCases());
        cases.addAll(relocationLifestyleDecisionCases());
        cases.addAll(casualSlangDecisionCases());

        assertEquals(3500, cases.size(), "human-like corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.text(), spec.analysisMode());
            try {
                spec.assertResult(result);
            } catch (AssertionError error) {
                failures.add("case " + i + " [" + spec.name() + "]: " + error.getMessage()
                        + "\nINPUT: " + spec.text()
                        + "\nEXTRACTED: " + result.getExtractedFields()
                        + "\nMISSING: " + result.getMissingCriticalFields()
                        + "\nSUMMARY: " + result.getSummary());
            }
        });

        assertTrue(failures.isEmpty(), String.join("\n\n", failures));
    }

    private List<CaseSpec> completeOfferCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currentCities = {
                new City("Austin, TX", "austin-tx", 42),
                new City("Phoenix, AZ", "phoenix-az", 44)
        };
        City[] offerCities = {
                new City("Seattle, WA", "seattle-wa", 60),
                new City("Los Angeles, CA", "los-angeles-ca", 58),
                new City("New York City", "new-york-ny", 64)
        };
        Unit[] units = {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("med surg telemetry", "med_surg")
        };
        for (int i = 0; i < 250; i++) {
            City current = currentCities[i % currentCities.length];
            City offer = offerCities[i % offerCities.length];
            Unit unit = units[i % units.length];
            int signOn = 9000 + (i % 9) * 1000;
            int relocation = 3000 + (i % 5) * 1000;
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "I am an RN in %s making $%.0f/hr. I got a %s %s offer at $%.0f/hr for 36 hours, nights, $%d sign-on, $%d relocation, 24-month commitment. Hospital-wide float and can cancel without pay.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation);
                case 1 -> String.format(Locale.US,
                        "Current job: RN in %s at $%.0f/hr. New %s RN offer in %s at $%.0f/hr. Sign-on bonus $%d. Relocation stipend $%d. 24 month commitment with prorated repayment. Hospital-wide float.",
                        current.label(), current.rate(), unit.label(), offer.label(), offer.rate(), signOn, relocation);
                case 2 -> String.format(Locale.US,
                        "I work in %s and make $%.0f/hr today. The offer is %s %s, $%.0f/hr, 36 hrs/wk, night shift, sign on bonus $%d, relocation assistance $%d, 24-month contract.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation);
                case 3 -> String.format(Locale.US,
                        "Currently based in %s at $%.0f/hr. Recruiter sent a %s role in %s with base rate $%.0f/hr, guaranteed 36 hours, $%d retention bonus, $%d moving reimbursement, 24 month service period.",
                        current.label(), current.rate(), unit.label(), offer.label(), offer.rate(), signOn, relocation);
                default -> String.format(Locale.US,
                        "My current RN job is in %s for $%.0f/hr. Offer is in %s for %s at $%.0f/hr, three 12s, nights, bonus $%d, relo $%d, 24-month commitment.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation);
            };
            cases.add(new CaseSpec("complete-offer-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(36.0, draft.getWeeklyHours(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(24, draft.getContractMonths());
                assertEquals(unit.slug(), draft.getUnitType());
            }));
        }
        return cases;
    }

    private List<CaseSpec> abbreviationCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "RN in Austin, TX making $42/hr. Seattle WA ICU offer $60/hr, 36h, NOC, SOB $15k, relo $4k, gtd hours, hospital-wide float.",
                "Currently Phoenix AZ at $44/hr. LA ED offer base $58/hr, three 12s, nocs, sob $7.5k, relocation $5k, float within service line.",
                "I am in Austin at $42/hr. NYC med surg tele offer is $64/hr, 36 hrs/wk, nights, sign-on $12k and relo $6k.",
                "Current: Phoenix, AZ $44/hr. Offer: Seattle, WA ICU $60/hr, gtd 36 hrs, night shift, bonus $10k, moving reimbursement $3k.",
                "Austin RN $42/hr now. New York City ED offer $64/hr, 36h/wk, NOC, commencement bonus $11k, relocation assistance $4k."
        };
        for (int i = 0; i < 100; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("abbrev-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 58.0);
                assertTrue(draft.getWeeklyHours() == 36.0 || result.getExtractedFields().stream()
                        .anyMatch(field -> field.contains("Scheduled hours")));
                assertTrue(draft.getSignOnBonus() >= 7500.0);
                assertTrue(draft.getRelocationStipend() >= 3000.0);
                assertTrue(draft.getOfferCitySlug().equals("seattle-wa")
                        || draft.getOfferCitySlug().equals("los-angeles-ca")
                        || draft.getOfferCitySlug().equals("new-york-ny"));
            }));
        }
        return cases;
    }

    private List<CaseSpec> lowInformationConcernCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "I got an ICU nurse offer in New York City but heard the unit has toxic culture and bullying.",
                "I have an offer but the income is lower and I like the coworkers, so I am torn.",
                "The bonus sounds good but I am worried about being short staffed and floating everywhere.",
                "I got an RN offer and the commute plus childcare might make this impossible.",
                "The recruiter says the team is supportive, but I am scared of unsafe ratios and burnout."
        };
        for (int i = 0; i < 75; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("concern-only-" + i, "offer_review", text, result -> {
                assertTrue(result.isParsed());
                assertTrue(result.getSummary().contains("not enough for a final verdict"));
                assertTrue(result.getMissingCriticalFields().contains("Offer hourly rate"));
                assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.startsWith("Concern:")
                        || field.startsWith("Positive tradeoff:")
                        || field.startsWith("Personal tradeoff:")));
            }));
        }
        return cases;
    }

    private List<CaseSpec> jobPostCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "Thinking about applying to a med surg telemetry RN job in Seattle, WA. Posted pay range is $49.23 to $91.22 per hour, 90% FTE nights, up to $10000 sign-on bonus.",
                "Los Angeles, CA ED registered nurse posting. Salary range $52.50 to $88.10 hourly. Night shift, three 12s, sign-on bonus $7500.",
                "New York City ICU RN job post: pay range $58.00 to $95.00 per hour, 36 hours per week, nights, relocation $5000.",
                "Seattle WA nurse opening, med surg tele, base rate $55/hr, 36 hrs/wk, weekend premium $4/hr, bonus $8000.",
                "Phoenix AZ ED RN hiring event. Pay Range Minimum $45.50 hourly Pay Range Maximum $72.20 hourly. NOC shift. Sign-On Bonus $6000."
        };
        for (int i = 0; i < 75; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("job-post-" + i, "job_post", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 45.0);
                assertTrue(draft.getSignOnBonus() >= 6000.0 || draft.getRelocationStipend() >= 5000.0);
                assertTrue(draft.getOfferCitySlug().equals("seattle-wa")
                        || draft.getOfferCitySlug().equals("los-angeles-ca")
                        || draft.getOfferCitySlug().equals("new-york-ny")
                        || draft.getOfferCitySlug().equals("phoenix-az"));
            }));
        }
        return cases;
    }

    private List<CaseSpec> completeConcernCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currentCities = {
                new City("Austin, TX", "austin-tx", 42),
                new City("Phoenix, AZ", "phoenix-az", 44)
        };
        City[] offerCities = {
                new City("Seattle, WA", "seattle-wa", 60),
                new City("Los Angeles, CA", "los-angeles-ca", 58),
                new City("New York City", "new-york-ny", 64)
        };
        Unit[] units = {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("med surg telemetry", "med_surg")
        };
        String[] worries = {
                "I heard the unit has toxic culture and bullying.",
                "I am worried about unsafe ratios and no breaks.",
                "The money looks better but the commute and childcare are scary.",
                "The coworkers seem good, but I worry the income is lower after rent.",
                "Hospital-wide float makes the offer feel risky."
        };
        for (int i = 0; i < 125; i++) {
            City current = currentCities[i % currentCities.length];
            City offer = offerCities[i % offerCities.length];
            Unit unit = units[i % units.length];
            int signOn = 10000 + (i % 6) * 1000;
            int relocation = 3000 + (i % 4) * 1000;
            String text = String.format(Locale.US,
                    "I am currently in %s making $%.0f/hr. Offer is a %s RN role in %s at $%.0f/hr, 36 hrs/wk, nights. Sign-on bonus $%d. Relocation stipend $%d. 24-month commitment. %s",
                    current.label(), current.rate(), unit.label(), offer.label(), offer.rate(), signOn, relocation,
                    worries[i % worries.length]);
            cases.add(new CaseSpec("complete-concern-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(36.0, draft.getWeeklyHours(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(24, draft.getContractMonths());
                assertEquals(unit.slug(), draft.getUnitType());
                assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.startsWith("Concern:")
                        || field.startsWith("Positive tradeoff:")
                        || field.startsWith("Personal tradeoff:")
                        || field.contains("Hospital-wide float")));
            }));
        }
        return cases;
    }

    private List<CaseSpec> messyStructuredNoteCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "current city Austin, TX\ncurrent pay $42/hr\nnew offer Seattle, WA ICU $60/hr\n36 hours\nsign-on $15,000\nrelocation $4,000\n24-month contract\nhospital-wide float",
                "CURRENT: Phoenix, AZ / $44/hr. OFFER: Los Angeles, CA ED / $58/hr / 36 hrs. Bonus: $12,000. Relo: $5,000. Contract: 24 months. Can cancel without pay.",
                "Me now = Austin, TX, $42/hr. New thing = New York City med surg telemetry, $64/hr, 36h/wk. Sign on bonus: $14,000. Relocation assistance: $6,000. 24 month commitment.",
                "RN offer notes: current Phoenix AZ $44/hr; Seattle WA ICU role base $60/hr; guaranteed 36 hours; retention bonus $11,000; moving reimbursement $3,000; prorated repayment.",
                "Austin RN making $42/hr now. LA ED offer base $58/hr. three 12s. nights. SOB $10k. relo $4k. 24-month service period."
        };
        for (int i = 0; i < 125; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("messy-note-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 58.0);
                assertTrue(draft.getSignOnBonus() >= 10000.0);
                assertTrue(draft.getRelocationStipend() >= 3000.0);
                assertTrue(draft.getOfferCitySlug().equals("seattle-wa")
                        || draft.getOfferCitySlug().equals("los-angeles-ca")
                        || draft.getOfferCitySlug().equals("new-york-ny"));
            }));
        }
        return cases;
    }

    private List<CaseSpec> moneyFormattingCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "I work in Austin, TX at $42/hr. Offer in Seattle, WA ICU at $60/hr. Sign-on bonus $15,000, relocation stipend $4,000, moving cost estimate $7,000, 24-month commitment.",
                "Currently Phoenix, AZ $44/hr. Offer Los Angeles, CA ED $58/hr, sign on $7.5k, relo $5k, moving cost $6k, 24 month commitment.",
                "Austin to New York City med surg tele: current $42/hr, offer $64/hr, 36 hrs/wk. Commencement bonus $11,500. Relocation assistance $4,500. Move estimate $8,000.",
                "Current Phoenix AZ at $44/hr. New Seattle WA ICU at $60/hr. $12,000 retention bonus. $3,000 moving reimbursement. 24-month service period.",
                "Austin RN $42/hr now; LA ED offer $58/hr; bonus $10,000; relocation $4,000; health insurance premium $250; current insurance $150."
        };
        for (int i = 0; i < 100; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("money-format-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 58.0);
                assertTrue(draft.getSignOnBonus() >= 7500.0);
                assertTrue(draft.getRelocationStipend() >= 3000.0);
                assertTrue(draft.getSignOnBonus() != draft.getRelocationStipend());
            }));
        }
        return cases;
    }

    private List<CaseSpec> decisionStageJobPostCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "Before applying: Seattle WA ICU RN posting. Pay Range $49.23 to $91.22 per hour. 90% FTE, nights, weekend premium $4/hr, sign-on bonus $10,000.",
                "Should I reply to this recruiter? LA ED RN opening. Salary range $52.50 to $88.10 hourly. three 12s, nocs, relocation $5,000.",
                "NYC med surg telemetry RN job post says $58.00 - $95.00 per hour, 36 hours per week, night shift, sign-on bonus up to $12k.",
                "Phoenix AZ ED hiring event, Pay Range Minimum $45.50 hourly Pay Range Maximum $72.20 hourly, Sign-On Bonus $6,000, float within service line.",
                "Seattle registered nurse opening, med surg tele unit, base rate $55/hr, 36 hrs/wk, weekend premium $4/hr, bonus $8,000."
        };
        for (int i = 0; i < 75; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("decision-job-post-" + i, "job_post", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 45.0);
                assertTrue(draft.getSignOnBonus() >= 6000.0 || draft.getRelocationStipend() >= 5000.0);
                assertTrue(draft.getUnitType().equals("icu")
                        || draft.getUnitType().equals("ed")
                        || draft.getUnitType().equals("med_surg"));
            }));
        }
        return cases;
    }

    private List<CaseSpec> partialDecisionAnxietyCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "I got an offer in New York City and I am scared because people say the unit has bullying. I do not know the hourly rate yet.",
                "The recruiter promised a bonus but I only know it is an ICU nurse job. I am worried about hospital-wide float.",
                "I may take a pay cut because the coworkers seem good, but I have not confirmed city or hourly rate.",
                "I have a Seattle RN offer but no written shift guarantee. The commute and childcare might make it fail.",
                "A Los Angeles ED role sounds exciting, but I heard unsafe ratios and low-census cancellation are common."
        };
        for (int i = 0; i < 75; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("partial-anxiety-" + i, "offer_review", text, result -> {
                assertTrue(result.isParsed());
                assertTrue(result.getSummary().contains("not enough for a final verdict")
                        || !result.getMissingCriticalFields().isEmpty());
                assertTrue(result.getMissingCriticalFields().contains("Offer hourly rate")
                        || result.getMissingCriticalFields().contains("Current hourly rate")
                        || result.getMissingCriticalFields().contains("Current city"));
                assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.startsWith("Concern:")
                        || field.startsWith("Positive tradeoff:")
                        || field.startsWith("Personal tradeoff:")
                        || field.contains("Shift not confirmed")
                        || field.contains("Hospital-wide float")));
            }));
        }
        return cases;
    }

    private List<CaseSpec> reversedOrderOfferCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currentCities = {
                new City("Austin, TX", "austin-tx", 42),
                new City("Phoenix, AZ", "phoenix-az", 44)
        };
        City[] offerCities = {
                new City("Seattle, WA", "seattle-wa", 60),
                new City("Los Angeles, CA", "los-angeles-ca", 58),
                new City("New York City", "new-york-ny", 64)
        };
        Unit[] units = {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("med surg telemetry", "med_surg")
        };
        for (int i = 0; i < 125; i++) {
            City current = currentCities[i % currentCities.length];
            City offer = offerCities[i % offerCities.length];
            Unit unit = units[i % units.length];
            int signOn = 9000 + (i % 7) * 1000;
            int relocation = 3000 + (i % 4) * 1000;
            String text = String.format(Locale.US,
                    "Offer first: %s %s RN, base rate $%.0f/hr, 36 hours, nights, sign-on $%d, relocation $%d, 24 month commitment. My current job is in %s at $%.0f/hr.",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
            cases.add(new CaseSpec("reversed-order-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(36.0, draft.getWeeklyHours(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(unit.slug(), draft.getUnitType());
            }));
        }
        return cases;
    }

    private List<CaseSpec> symbolHeavyMobileNoteCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "current/Austin TX/$42/hr -> offer/Seattle WA/ICU/$60/hr/36h/sign-on=$15k/relo=$4k/24mo",
                "now Phoenix AZ $44/hr >>> LA ED offer $58/hr | 36 hrs | SOB=$7500 | relo=$5000 | float within service line",
                "Austin $42/hr rn now -> NYC med surg tele $64/hr, 36h, nights, bonus=$12000, relocation=$6000",
                "Current=Phoenix AZ @ $44/hr; Offer=Seattle WA ICU @ $60/hr; gtd 36 hrs; retention bonus=$11000; moving reimbursement=$3000",
                "Austin RN $42/hr now; Los Angeles CA ED offer $58/hr; three 12s; sign on=$10k; relocation assistance=$4k"
        };
        for (int i = 0; i < 125; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("symbol-mobile-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 58.0);
                assertTrue(draft.getSignOnBonus() >= 7500.0);
                assertTrue(draft.getRelocationStipend() >= 3000.0);
                assertTrue(draft.getOfferCitySlug().equals("seattle-wa")
                        || draft.getOfferCitySlug().equals("los-angeles-ca")
                        || draft.getOfferCitySlug().equals("new-york-ny"));
            }));
        }
        return cases;
    }

    private List<CaseSpec> shiftPremiumAndBonusSeparationCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "Austin, TX RN making $42/hr. Seattle, WA ICU offer $60/hr, 36 hours. Night shift premium $6/hr. Weekend premium $4/hr. Sign-on bonus $15000. Relocation $4000.",
                "Phoenix AZ current $44/hr. LA ED offer base $58/hr, three 12s. Night differential 12%. Weekend differential 8%. Bonus $10000. Relo $5000.",
                "I make $42/hr in Austin. New York City med surg tele offer is $64/hr. Night premium $5/hr, weekend premium $4/hr, commencement bonus $11500, relocation assistance $4500.",
                "Current Phoenix AZ $44/hr. Seattle ICU base rate $60/hr. Weekend premium $4.00/hour. Night premium $6.80/hour. Sign-on bonus $12000.",
                "Austin RN now $42/hr; LA ED offer $58/hr; shift premium $5/hr; sign-on bonus $10000; relocation stipend $4000."
        };
        for (int i = 0; i < 100; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("premium-separation-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 58.0);
                assertTrue(draft.getSignOnBonus() >= 10000.0);
                assertTrue(draft.getSignOnBonus() > 250.0);
                assertTrue(draft.getSignOnBonus() != draft.getNightDiffPercent());
                assertTrue(draft.getRelocationStipend() == 0.0 || draft.getRelocationStipend() >= 4000.0);
            }));
        }
        return cases;
    }

    private List<CaseSpec> ambiguousMoneyButMissingBasicsCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "The recruiter mentioned $15000 bonus and $5000 relocation, but I do not know the city or hourly rate yet. I am worried about float.",
                "I got told there is a big sign-on bonus, maybe $12000, but no written hours or shift guarantee. The unit sounds toxic.",
                "Offer has $4000 relocation and health insurance premium $250, but I do not have base pay yet. Worried about childcare.",
                "They said weekend premium is $4/hr and night premium is $6/hr, but I do not know the actual hourly rate. Is this a trap?",
                "I only know it is an ICU offer with bonus $10000 and possible low census cancellation."
        };
        for (int i = 0; i < 75; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("ambiguous-money-" + i, "offer_review", text, result -> {
                assertTrue(result.isParsed());
                assertTrue(!result.getMissingCriticalFields().isEmpty());
                assertTrue(result.getMissingCriticalFields().contains("Offer hourly rate")
                        || result.getMissingCriticalFields().contains("Offer city")
                        || result.getMissingCriticalFields().contains("Current city")
                        || result.getMissingCriticalFields().contains("Current hourly rate"));
                assertTrue(result.getSummary().contains("not enough for a final verdict")
                        || result.getSummary().contains("Review these before running the report"));
            }));
        }
        return cases;
    }

    private List<CaseSpec> jobPostNoiseCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "About the hospital: population served 500000. Seattle WA ICU RN listing. Pay range $49.23 - $91.22/hr. Night shift. Sign-on bonus $10000. Nearby cities: Portland 174 miles.",
                "Los Angeles CA ED RN opening. Salary Range $52.50 to $88.10 hourly. Parking $120/month. Night premium $6/hr. Relocation $5000.",
                "NYC med surg tele nurse post. Pay Range Minimum $58.00 hourly Pay Range Maximum $95.00 hourly. 36 hours. Weekend premium $4/hr. Bonus $12000.",
                "Phoenix AZ ED RN hiring event. Pay range $45.50 - $72.20/hr. Facility is 12 miles from downtown. Sign-On Bonus $6000.",
                "Seattle WA registered nurse opening. Base rate $55/hr. 36 hrs/wk. Cost of living blurb mentions New York and Los Angeles but job is Seattle. Bonus $8000."
        };
        for (int i = 0; i < 75; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("job-post-noise-" + i, "job_post", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertTrue(draft.getOfferHourlyRate() >= 45.0);
                assertTrue(draft.getSignOnBonus() >= 6000.0 || draft.getRelocationStipend() >= 5000.0);
                assertTrue(draft.getOfferCitySlug().equals("seattle-wa")
                        || draft.getOfferCitySlug().equals("los-angeles-ca")
                        || draft.getOfferCitySlug().equals("new-york-ny")
                        || draft.getOfferCitySlug().equals("phoenix-az"));
            }));
        }
        return cases;
    }

    private List<CaseSpec> humanDoubtWithCompleteFactsCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currents = humanCurrentCities();
        City[] offers = humanOfferCities();
        Unit[] units = humanUnits();
        String[] concerns = {
                "I am worried this is secretly a pay cut after benefits and parking.",
                "I heard the unit is short staffed and people say the culture is rough.",
                "The manager seems good but I do not know if that offsets the move.",
                "I like my coworkers now, so I am scared I am chasing a higher hourly rate only.",
                "Childcare and the commute are the part I keep overthinking."
        };
        for (int i = 0; i < 500; i++) {
            City current = currents[i % currents.length];
            City offer = offers[(i * 2) % offers.length];
            Unit unit = units[i % units.length];
            int signOn = 8000 + (i % 8) * 1000;
            int relocation = 2500 + (i % 6) * 500;
            int moving = 4200 + (i % 7) * 400;
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "I am currently in %s making $%.0f/hr. I got a written %s RN offer in %s at $%.0f/hr, 36 hrs/wk, nights, sign-on $%d, relocation $%d, moving cost estimate $%d. %s",
                        current.label(), current.rate(), unit.label(), offer.label(), offer.rate(), signOn, relocation,
                        moving, concerns[i % concerns.length]);
                case 1 -> String.format(Locale.US,
                        "Trying to decide like a normal person: current job %s $%.0f/hr, new offer city %s %s at $%.0f/hr, three 12s, bonus $%d, relo $%d, move estimate $%d. %s",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving, concerns[i % concerns.length]);
                case 2 -> String.format(Locale.US,
                        "My current RN job is in %s for $%.0f/hr. Recruiter offer located in %s for %s, base rate $%.0f/hr, guaranteed 36 hours, retention bonus $%d, relocation stipend $%d, moving cost $%d. %s",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving, concerns[i % concerns.length]);
                case 3 -> String.format(Locale.US,
                        "I work in %s and make $%.0f/hr today. New role in %s is %s at $%.0f/hr with 36 hours, SOB $%d, relocation assistance $%d, moving expense $%d. %s",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving, concerns[i % concerns.length]);
                default -> String.format(Locale.US,
                        "Current city %s, current pay $%.0f/hr. Offer city %s, unit %s, offer pay $%.0f/hr, 36h, sign-on bonus $%d, relocation support $%d, move cost=$%d. %s",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving, concerns[i % concerns.length]);
            };
            cases.add(new CaseSpec("human-complete-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(36.0, draft.getWeeklyHours(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(moving, draft.getMovingCost(), 0.01);
                assertEquals(unit.slug(), draft.getUnitType());
                assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.startsWith("Concern:")
                        || field.startsWith("Positive tradeoff:")
                        || field.startsWith("Personal tradeoff:")));
            }));
        }
        return cases;
    }

    private List<CaseSpec> recruiterThreadPasteCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currents = humanCurrentCities();
        City[] offers = humanOfferCities();
        Unit[] units = humanUnits();
        String[] floatPhrases = {
                "float within service line",
                "hospital-wide float",
                "home unit only",
                "subject to cancellation",
                "guaranteed hours"
        };
        for (int i = 0; i < 500; i++) {
            City current = currents[(i + 1) % currents.length];
            City offer = offers[(i * 3 + 1) % offers.length];
            Unit unit = units[(i + 2) % units.length];
            int signOn = 9000 + (i % 5) * 1500;
            int relocation = 3000 + (i % 4) * 750;
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Recruiter text pasted: role=%s RN, location=%s, base rate $%.0f/hr, 36 hours, sign on bonus $%d, relocation $%d. Me now: %s $%.0f/hr. %s.",
                        unit.label(), offer.label(), offer.rate(), signOn, relocation, current.label(), current.rate(),
                        floatPhrases[i % floatPhrases.length]);
                case 1 -> String.format(Locale.US,
                        "From recruiter: new %s nurse job in %s at $%.0f/hr, 3x12 nights, bonus maybe $%d, $%d relocation stipend. Currently %s rn $%.0f/hr. %s.",
                        unit.label(), offer.label(), offer.rate(), signOn, relocation, current.label(), current.rate(),
                        floatPhrases[i % floatPhrases.length]);
                case 2 -> String.format(Locale.US,
                        "SMS thread says offer is located in %s for %s at $%.0f/hr. Sign-on amount $%d. Moving reimbursement $%d. I currently work in %s making $%.0f/hr. %s.",
                        offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate(),
                        floatPhrases[i % floatPhrases.length]);
                case 3 -> String.format(Locale.US,
                        "Recruiter note - current: %s / $%.0f hourly. new rn job: %s / %s / $%.0f hourly / 36h / sob $%d / relo $%d / %s.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        floatPhrases[i % floatPhrases.length]);
                default -> String.format(Locale.US,
                        "They sent this: %s registered nurse opening in %s. Compensation $%.0f/hr. Bonus $%d. Relocation assistance $%d. My current job is in %s making $%.0f/hr. %s.",
                        unit.label(), offer.label(), offer.rate(), signOn, relocation, current.label(), current.rate(),
                        floatPhrases[i % floatPhrases.length]);
            };
            cases.add(new CaseSpec("recruiter-thread-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(unit.slug(), draft.getUnitType());
                assertTrue(!draft.getFloatRisk().isBlank() || !draft.getCancelRisk().isBlank()
                        || !draft.getShiftGuarantee().isBlank());
            }));
        }
        return cases;
    }

    private List<CaseSpec> jobPostPasteWithHumanQuestionCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] offers = humanOfferCities();
        Unit[] units = humanUnits();
        String[] leadIns = {
                "Can you sanity check this before I apply?",
                "Pasted from PDF:",
                "Job listing copied from recruiter portal:",
                "I have not applied yet but this posting caught my eye:",
                "Is this posting worth a call?"
        };
        for (int i = 0; i < 500; i++) {
            City offer = offers[(i * 5) % offers.length];
            Unit unit = units[(i + 1) % units.length];
            double low = offer.rate() - 8 + (i % 4);
            double high = offer.rate() + 18 + (i % 6);
            int signOn = 6000 + (i % 7) * 1000;
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "%s %s registered nurse, %s; range minimum $%.2f; range maximum $%.2f; sign on bonus $%d; 36 hours.",
                        leadIns[i % leadIns.length], offer.label(), unit.label(), low, high, signOn);
                case 1 -> String.format(Locale.US,
                        "%s %s %s RN posting. Pay Range Minimum: $%.2f hourly. Pay Range Maximum: $%.2f hourly. Nights, 3x12, sign-on bonus up to $%d.",
                        leadIns[i % leadIns.length], offer.label(), unit.label(), low, high, signOn);
                case 2 -> String.format(Locale.US,
                        "%s Job post says location %s, unit %s, pay range $%.2f to $%.2f per hour, weekend premium $4/hr, bonus $%d.",
                        leadIns[i % leadIns.length], offer.label(), unit.label(), low, high, signOn);
                case 3 -> String.format(Locale.US,
                        "%s Open role %s %s nurse. Posted pay $%.2f - $%.2f/hr. 90%% FTE nights. Relocation $5000.",
                        leadIns[i % leadIns.length], offer.label(), unit.label(), low, high);
                default -> String.format(Locale.US,
                        "%s %s registered nurse opening, %s department, range minimum $%.2f range maximum $%.2f, sign-on $%d.",
                        leadIns[i % leadIns.length], offer.label(), unit.label(), low, high, signOn);
            };
            cases.add(new CaseSpec("job-post-human-question-" + i, "job_post", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(low, draft.getOfferHourlyRate(), 0.01);
                assertEquals(unit.slug(), draft.getUnitType());
                assertTrue(draft.getSignOnBonus() >= 6000.0 || draft.getRelocationStipend() >= 5000.0);
                assertTrue(result.getSummary().contains("job post") || result.getExtractedFields().stream()
                        .anyMatch(field -> field.contains("Posted pay range")));
            }));
        }
        return cases;
    }

    private List<CaseSpec> lowInfoPersonalTradeoffCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "I got an offer but I only know the team seems supportive. I am worried the income is lower after benefits.",
                "I am in decision paralysis. The manager seems normal, coworkers seem good, but I have no hourly rate yet.",
                "The unit has a reputation for bullying and short staffing. I know it is an ICU role but not the pay.",
                "I want to be closer to family and childcare would be easier, but I do not have the written offer details.",
                "They mentioned a bonus and relocation, but I am worried about low census cancellation and unsafe ratios."
        };
        for (int i = 0; i < 500; i++) {
            String text = texts[i % texts.length];
            cases.add(new CaseSpec("low-info-human-tradeoff-" + i, "offer_review", text, result -> {
                assertTrue(result.isParsed());
                assertTrue(!result.getMissingCriticalFields().isEmpty());
                assertTrue(result.getMissingCriticalFields().contains("Offer hourly rate")
                        || result.getMissingCriticalFields().contains("Offer city")
                        || result.getMissingCriticalFields().contains("Current hourly rate"));
                assertTrue(result.getSummary().contains("not enough for a final verdict")
                        || result.getSummary().contains("Review these before running the report"));
                assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.startsWith("Concern:")
                        || field.startsWith("Positive tradeoff:")
                        || field.startsWith("Personal tradeoff:")
                        || field.contains("Cancellation")
                        || field.contains("ICU")));
            }));
        }
        return cases;
    }

    private List<CaseSpec> moneyLabelConfusionCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currents = humanCurrentCities();
        City[] offers = humanOfferCities();
        Unit[] units = humanUnits();
        for (int i = 0; i < 500; i++) {
            City current = currents[(i + 2) % currents.length];
            City offer = offers[(i + 3) % offers.length];
            Unit unit = units[(i + 3) % units.length];
            int signOn = 10000 + (i % 6) * 1250;
            int relocation = 3000 + (i % 5) * 600;
            int moving = 4500 + (i % 4) * 700;
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Current %s $%.0f/hr. Offer %s %s $%.0f/hr. Night premium $6/hr, weekend premium $4/hr, bonus maybe $%d, relocation stipend $%d, moving cost estimate $%d.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving);
                case 1 -> String.format(Locale.US,
                        "I make $%.0f/hr in %s. New role in %s pays $%.0f/hr, %s, 36h. Sign-on amount: $%d. $%d relocation assistance. Move estimate ($%d).",
                        current.rate(), current.label(), offer.label(), offer.rate(), unit.label(), signOn, relocation,
                        moving);
                case 2 -> String.format(Locale.US,
                        "Me now %s $%.0f hourly. Offer city %s unit %s rate $%.0f hourly. Weekend diff $4.50/hr and night shift premium $7/hr. Retention bonus $%d. ReLo $%d. Moving expense $%d.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving);
                case 3 -> String.format(Locale.US,
                        "%s at $%.0f/hr now; %s %s offer at $%.0f/hr; sign on=$%d; relocation support=$%d; parking $180/mo; moving cost $%d.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving);
                default -> String.format(Locale.US,
                        "Current job %s rate $%.0f. New RN job %s %s rate $%.0f per hour. Bonus $%d / relo $%d / move cost $%d / insurance premium $260.",
                        current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                        moving);
            };
            cases.add(new CaseSpec("money-label-confusion-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(moving, draft.getMovingCost(), 0.01);
                assertEquals(unit.slug(), draft.getUnitType());
            }));
        }
        return cases;
    }

    private List<CaseSpec> relocationLifestyleDecisionCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currents = humanCurrentCities();
        City[] offers = humanOfferCities();
        Unit[] units = humanUnits();
        String[] tradeoffs = {
                "I would be closer to family but rent is higher.",
                "My commute would be shorter but daycare is more complicated.",
                "The team seems good, but I am leaving coworkers I actually like.",
                "I am scared the move cost is bigger than the bonus.",
                "The location is better for my partner but worse for my schedule."
        };
        for (int i = 0; i < 500; i++) {
            City current = currents[(i + 3) % currents.length];
            City offer = offers[(i + 4) % offers.length];
            Unit unit = units[(i + 4) % units.length];
            int signOn = 7000 + (i % 6) * 1000;
            int relocation = 2500 + (i % 6) * 700;
            int moving = 5000 + (i % 6) * 500;
            String text = String.format(Locale.US,
                    "I currently live in %s and work for $%.0f/hr. The offer is located in %s for %s at $%.0f/hr, 36 hrs, sign-on bonus $%d, relocation $%d, moving cost estimate $%d. %s",
                    current.label(), current.rate(), offer.label(), unit.label(), offer.rate(), signOn, relocation,
                    moving, tradeoffs[i % tradeoffs.length]);
            cases.add(new CaseSpec("relocation-lifestyle-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(moving, draft.getMovingCost(), 0.01);
                assertEquals(unit.slug(), draft.getUnitType());
                assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.startsWith("Positive tradeoff:")
                        || field.startsWith("Personal tradeoff:")
                        || field.startsWith("Concern:")));
            }));
        }
        return cases;
    }

    private List<CaseSpec> casualSlangDecisionCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] currents = humanCurrentCities();
        City[] offers = humanOfferCities();
        Unit[] units = humanUnits();
        String[] slang = {
                "ngl I am spiraling a little",
                "lowkey this sounds good but also sus",
                "idk if the vibes are enough",
                "be honest, am I being dumb",
                "the money looks cute but the culture rumors are not"
        };
        for (int i = 0; i < 500; i++) {
            City current = currents[(i + 4) % currents.length];
            City offer = offers[(i + 5) % offers.length];
            Unit unit = units[(i + 1) % units.length];
            int signOn = 8000 + (i % 5) * 1000;
            int relocation = 3000 + (i % 5) * 500;
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "%s. rn now %s $%.0f/hr, new offer city is %s %s at $%.0f/hr, 36h, sob $%d, relo $%d, heard unsafe ratios.",
                        slang[i % slang.length], current.label(), current.rate(), offer.label(), unit.label(),
                        offer.rate(), signOn, relocation);
                case 1 -> String.format(Locale.US,
                        "%s: current job %s $%.0f hourly -> new role in %s %s pays $%.0f hourly, three 12s, bonus $%d, relocation $%d, coworkers seem good.",
                        slang[i % slang.length], current.label(), current.rate(), offer.label(), unit.label(),
                        offer.rate(), signOn, relocation);
                case 2 -> String.format(Locale.US,
                        "%s. me now %s $%.0f/hr. recruiter offer located in %s for %s, $%.0f/hr, sign-on $%d, relocation assistance $%d, toxic culture rumor.",
                        slang[i % slang.length], current.label(), current.rate(), offer.label(), unit.label(),
                        offer.rate(), signOn, relocation);
                case 3 -> String.format(Locale.US,
                        "%s - current city %s rate $%.0f/hr; offer city %s unit %s base rate $%.0f/hr; bonus maybe $%d; $%d relocation stipend.",
                        slang[i % slang.length], current.label(), current.rate(), offer.label(), unit.label(),
                        offer.rate(), signOn, relocation);
                default -> String.format(Locale.US,
                        "%s. I am in %s making $%.0f/hr and the new %s RN job in %s is $%.0f/hr with $%d bonus and $%d relo.",
                        slang[i % slang.length], current.label(), current.rate(), unit.label(), offer.label(),
                        offer.rate(), signOn, relocation);
            };
            cases.add(new CaseSpec("casual-slang-" + i, "offer_review", text, result -> {
                OfferRiskDraft draft = result.getDraft();
                assertTrue(result.isParsed());
                assertEquals(current.slug(), draft.getCurrentCitySlug());
                assertEquals(offer.slug(), draft.getOfferCitySlug());
                assertEquals(current.rate(), draft.getCurrentHourlyRate(), 0.01);
                assertEquals(offer.rate(), draft.getOfferHourlyRate(), 0.01);
                assertEquals(signOn, draft.getSignOnBonus(), 0.01);
                assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                assertEquals(unit.slug(), draft.getUnitType());
                assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.startsWith("Concern:")
                        || field.startsWith("Positive tradeoff:"))
                        || result.getSummary().contains("Auto-filled"));
            }));
        }
        return cases;
    }

    private City[] humanCurrentCities() {
        return new City[] {
                new City("Austin, TX", "austin-tx", 42),
                new City("Phoenix, AZ", "phoenix-az", 44),
                new City("Chicago, IL", "chicago-il", 46),
                new City("Houston, TX", "houston-tx", 43),
                new City("Miami, FL", "miami-fl", 45)
        };
    }

    private City[] humanOfferCities() {
        return new City[] {
                new City("Seattle, WA", "seattle-wa", 60),
                new City("Los Angeles, CA", "los-angeles-ca", 58),
                new City("New York City", "new-york-ny", 64),
                new City("Boston, MA", "boston-ma", 61),
                new City("Philadelphia, PA", "philadelphia-pa", 57)
        };
    }

    private Unit[] humanUnits() {
        return new Unit[] {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("med surg telemetry", "med_surg"),
                new Unit("operating room", "or"),
                new Unit("labor and delivery", "l_and_d")
        };
    }

    private record City(String label, String slug, double rate) {
    }

    private record Unit(String label, String slug) {
    }

    private record CaseSpec(String name, String analysisMode, String text, ResultAssertion assertion) {
        private void assertResult(OfferTextParseResult result) {
            assertion.assertResult(result);
        }
    }

    @FunctionalInterface
    private interface ResultAssertion {
        void assertResult(OfferTextParseResult result);
    }
}
