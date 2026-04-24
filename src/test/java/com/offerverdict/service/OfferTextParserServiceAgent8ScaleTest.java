package com.offerverdict.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.OfferRiskDraft;
import com.offerverdict.model.OfferTextParseResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OfferTextParserServiceAgent8ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports", "agent8-parser-failures.txt");

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandConcernFirstSparseFactInputs() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(toxicCultureSparseOfferCases());
        cases.addAll(floatCancelFearCases());
        cases.addAll(lowerTakeHomeCases());
        cases.addAll(childcareCommuteTradeoffCases());
        cases.addAll(mixedAnxietyMinimalFactCases());

        assertEquals(1000, cases.size(), "Agent 8 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) {
        List<Failure> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.input(), "offer_review");
            List<String> mismatches = spec.validate(result);
            if (!mismatches.isEmpty()) {
                failures.add(new Failure(i, spec, result, mismatches));
            }
        });

        if (!failures.isEmpty()) {
            writeFailureReport(failures);
            Map<String, Long> categories = failures.stream()
                    .flatMap(failure -> failure.categories().stream())
                    .collect(Collectors.groupingBy(category -> category, LinkedHashMap::new, Collectors.counting()));
            fail("Agent 8 concern-first parser failures: pass=" + (cases.size() - failures.size())
                    + ", fail=" + failures.size()
                    + ", unique failure categories=" + categories.keySet()
                    + ", category counts=" + categories
                    + ". Full report: " + FAILURE_REPORT.toAbsolutePath()
                    + "\n\n" + failures.stream().limit(40).map(Failure::format).collect(Collectors.joining("\n\n")));
        }

        deleteStaleFailureReport();
        assertTrue(failures.isEmpty(), "pass=" + cases.size() + ", fail=0");
    }

    private List<CaseSpec> toxicCultureSparseOfferCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City offer = offerCities()[i % offerCities().length];
            Unit unit = units()[i % units().length];
            String culture = toxicConcerns()[i % toxicConcerns().length];
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "%s and I cannot tell if I am overreacting. Sparse facts: offer city is %s, unit is %s.",
                        culture, offer.label(), unit.label());
                case 1 -> String.format(Locale.US,
                        "Main issue first: %s. The only concrete thing I know is a %s RN offer in %s.",
                        culture, unit.label(), offer.label());
                case 2 -> String.format(Locale.US,
                        "Before pay details, my anxiety is %s. Recruiter says the new role is located in %s on %s.",
                        culture, offer.label(), unit.label());
                case 3 -> String.format(Locale.US,
                        "%s; I am trying to decide without the whole letter. Offer: %s, %s nurse.",
                        culture, offer.label(), unit.label());
                default -> String.format(Locale.US,
                        "Concern-first note: %s. The sparse facts are offer city=%s and department=%s.",
                        culture, offer.label(), unit.label());
            };
            cases.add(new CaseSpec("toxic-culture-" + i, input, offer.slug(), null, unit.slug(),
                    List.of("Concern:"), List.of("Offer hourly rate", "Current city", "Current hourly rate")));
        }
        return cases;
    }

    private List<CaseSpec> floatCancelFearCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City offer = offerCities()[(i + 1) % offerCities().length];
            double rate = 54 + (i % 12);
            String opsConcern = operationalConcerns()[i % operationalConcerns().length];
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "I am mostly scared they will %s. Facts are thin: offer in %s at $%.0f/hr.",
                        opsConcern, offer.label(), rate);
                case 1 -> String.format(Locale.US,
                        "The anxiety is not the base rate, it is that they can %s. New role located in %s pays $%.0f/hr.",
                        opsConcern, offer.label(), rate);
                case 2 -> String.format(Locale.US,
                        "My fear: %s after I resign from a stable job. Offer city %s, hourly $%.0f/hr.",
                        opsConcern, offer.label(), rate);
                case 3 -> String.format(Locale.US,
                        "I keep thinking about whether they %s. Sparse facts: %s RN offer, $%.0f/hr.",
                        opsConcern, offer.label(), rate);
                default -> String.format(Locale.US,
                        "Concern first, details second: %s. The offer is %s at $%.0f per hour.",
                        opsConcern, offer.label(), rate);
            };
            cases.add(new CaseSpec("float-cancel-" + i, input, offer.slug(), rate, null,
                    List.of("Float terms:", "Cancellation terms:"), List.of("Current city", "Current hourly rate")));
        }
        return cases;
    }

    private List<CaseSpec> lowerTakeHomeCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City offer = offerCities()[(i + 2) % offerCities().length];
            City current = currentCities()[i % currentCities().length];
            double currentRate = 45 + (i % 7);
            double offerRate = 50 + (i % 9);
            String payConcern = payConcerns()[i % payConcerns().length];
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "%s because benefits and parking may eat it up. Currently %s at $%.0f/hr; offer %s $%.0f/hr.",
                        payConcern, current.label(), currentRate, offer.label(), offerRate);
                case 1 -> String.format(Locale.US,
                        "I am worried the take-home is lower even if the headline pay is okay. Current: %s $%.0f/hr. Offer: %s $%.0f/hr.",
                        current.label(), currentRate, offer.label(), offerRate);
                case 2 -> String.format(Locale.US,
                        "%s. My current job is in %s making $%.0f/hr, new offer city is %s at $%.0f/hr.",
                        payConcern, current.label(), currentRate, offer.label(), offerRate);
                case 3 -> String.format(Locale.US,
                        "The anxiety is less money after deductions. I work in %s at $%.0f/hr now; offer in %s pays $%.0f/hr.",
                        current.label(), currentRate, offer.label(), offerRate);
                default -> String.format(Locale.US,
                        "%s. Sparse facts: now in %s making $%.0f/hr, new role in %s pays $%.0f/hr.",
                        payConcern, current.label(), currentRate, offer.label(), offerRate);
            };
            cases.add(new CaseSpec("lower-take-home-" + i, input, offer.slug(), offerRate, null,
                    List.of("Concern: lower take-home pay"), List.of("Moving cost estimate")));
        }
        return cases;
    }

    private List<CaseSpec> childcareCommuteTradeoffCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City offer = offerCities()[(i + 3) % offerCities().length];
            Unit unit = units()[(i + 2) % units().length];
            String familyConcern = familyConcerns()[i % familyConcerns().length];
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "%s. I only know the offer is a %s role in %s.",
                        familyConcern, unit.label(), offer.label());
                case 1 -> String.format(Locale.US,
                        "My first concern is childcare and the school schedule, not the title. Offer location %s, unit %s.",
                        offer.label(), unit.label());
                case 2 -> String.format(Locale.US,
                        "I am trying to protect kid pickup and daycare. The sparse job facts: %s %s RN.",
                        offer.label(), unit.label());
                case 3 -> String.format(Locale.US,
                        "%s; recruiter only gave me %s as the city and %s as the unit.",
                        familyConcern, offer.label(), unit.label());
                default -> String.format(Locale.US,
                        "Concern-first: better commute sounds good but childcare may break. Offer is in %s for %s.",
                        offer.label(), unit.label());
            };
            cases.add(new CaseSpec("childcare-commute-" + i, input, offer.slug(), null, unit.slug(),
                    List.of("Personal tradeoff:"), List.of("Offer hourly rate", "Current city", "Current hourly rate")));
        }
        return cases;
    }

    private List<CaseSpec> mixedAnxietyMinimalFactCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City offer = offerCities()[(i + 4) % offerCities().length];
            double rate = 56 + (i % 10);
            String culture = toxicConcerns()[i % toxicConcerns().length];
            String staffing = staffingConcerns()[i % staffingConcerns().length];
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "%s, and also %s. Concrete fact buried at the end: offer in %s at $%.0f/hr.",
                        culture, staffing, offer.label(), rate);
                case 1 -> String.format(Locale.US,
                        "I am anxious about burnout, %s, and whether I am walking into %s. New job: %s, $%.0f/hr.",
                        staffing, culture, offer.label(), rate);
                case 2 -> String.format(Locale.US,
                        "Mostly fear, very few facts: %s; %s. Offer city is %s, base rate is $%.0f/hr.",
                        culture, staffing, offer.label(), rate);
                case 3 -> String.format(Locale.US,
                        "%s plus %s are the reasons I am hesitating. The offer says %s and $%.0f per hour.",
                        staffing, culture, offer.label(), rate);
                default -> String.format(Locale.US,
                        "Concern-first dump: %s, %s, and maybe lower take-home is lower. Sparse fact: %s $%.0f/hr.",
                        culture, staffing, offer.label(), rate);
            };
            cases.add(new CaseSpec("mixed-anxiety-" + i, input, offer.slug(), rate, null,
                    List.of("Concern:"), List.of("Current city", "Current hourly rate")));
        }
        return cases;
    }

    private static City[] offerCities() {
        return new City[] {
                new City("Seattle, WA", "seattle-wa"),
                new City("Los Angeles, CA", "los-angeles-ca"),
                new City("New York City", "new-york-ny"),
                new City("Boston, MA", "boston-ma"),
                new City("Philadelphia, PA", "philadelphia-pa"),
                new City("Phoenix, AZ", "phoenix-az")
        };
    }

    private static City[] currentCities() {
        return new City[] {
                new City("Austin, TX", "austin-tx"),
                new City("Chicago, IL", "chicago-il"),
                new City("Houston, TX", "houston-tx"),
                new City("Miami, FL", "miami-fl")
        };
    }

    private static Unit[] units() {
        return new Unit[] {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("operating room", "or"),
                new Unit("labor and delivery", "l_and_d"),
                new Unit("clinic", "clinic"),
                new Unit("float pool", "float_pool"),
                new Unit("med surg tele", "med_surg")
        };
    }

    private static String[] toxicConcerns() {
        return new String[] {
                "I heard the unit has toxic culture",
                "the old staff warned me about bullying",
                "people keep describing a hostile culture",
                "people keep calling it an unsafe culture",
                "two nurses called it a dumpster fire",
                "I am scared of lateral violence",
                "the vibe check says mean girl behavior",
                "everyone hints that nurses eat their young"
        };
    }

    private static String[] operationalConcerns() {
        return new String[] {
                "float to any unit and can cancel without pay",
                "send me across the hospital and hours not guaranteed",
                "float me everywhere and cancel me first",
                "use hospital-wide float with low census cancellation",
                "move me throughout the hospital and subject to cancellation",
                "put me in floating everywhere while they can cancel without pay"
        };
    }

    private static String[] payConcerns() {
        return new String[] {
                "I am afraid this is a pay cut",
                "I keep calculating that the income is lower",
                "The offer may mean less money",
                "My take-home is lower in every rough budget",
                "I may make less after benefits",
                "This pays less once premiums and parking hit"
        };
    }

    private static String[] familyConcerns() {
        return new String[] {
                "Childcare is the thing making me hesitate",
                "The commute could wreck kid pickup",
                "Daycare timing may make the schedule impossible",
                "A shorter commute would help, but school schedule is tight",
                "I need the job to fit childcare and family logistics",
                "Better commute matters, but daycare pickup is fragile"
        };
    }

    private static String[] staffingConcerns() {
        return new String[] {
                "short staffed nights",
                "unsafe ratios",
                "too many patients",
                "no breaks",
                "no lunch",
                "ratios sound sketchy"
        };
    }

    private static void writeFailureReport(List<Failure> failures) {
        try {
            Files.createDirectories(FAILURE_REPORT.getParent());
            Files.writeString(FAILURE_REPORT, failures.stream()
                    .map(Failure::format)
                    .collect(Collectors.joining("\n\n")));
        } catch (IOException ignored) {
            // The assertion message still includes the first failures if report writing is unavailable.
        }
    }

    private static void deleteStaleFailureReport() {
        try {
            Files.deleteIfExists(FAILURE_REPORT);
        } catch (IOException ignored) {
            // A stale report should not fail an otherwise clean parser run.
        }
    }

    private record City(String label, String slug) {
    }

    private record Unit(String label, String slug) {
    }

    private record CaseSpec(String name,
                            String input,
                            String offerCitySlug,
                            Double offerHourlyRate,
                            String unitType,
                            List<String> expectedExtractedPrefixes,
                            List<String> expectedMissingFields) {

        private List<String> validate(OfferTextParseResult result) {
            OfferRiskDraft draft = result.getDraft();
            List<String> mismatches = new ArrayList<>();

            if (!result.isParsed()) {
                mismatches.add("parsed expected <true> but was <false>");
            }
            if (!offerCitySlug.equals(draft.getOfferCitySlug())) {
                mismatches.add("offerCitySlug expected <" + offerCitySlug + "> but was <"
                        + draft.getOfferCitySlug() + ">");
            }
            if (offerHourlyRate != null && Math.abs(offerHourlyRate - draft.getOfferHourlyRate()) > 0.01) {
                mismatches.add("offerHourlyRate expected <" + offerHourlyRate + "> but was <"
                        + draft.getOfferHourlyRate() + ">");
            }
            if (unitType != null && !unitType.equals(draft.getUnitType())) {
                mismatches.add("unitType expected <" + unitType + "> but was <" + draft.getUnitType() + ">");
            }
            for (String prefix : expectedExtractedPrefixes) {
                if (result.getExtractedFields().stream().noneMatch(field -> field.startsWith(prefix))) {
                    mismatches.add("extractedFields expected prefix <" + prefix + "> but was "
                            + result.getExtractedFields());
                }
            }
            for (String missingField : expectedMissingFields) {
                if (!result.getMissingCriticalFields().contains(missingField)) {
                    mismatches.add("missingCriticalFields expected to contain <" + missingField + "> but was "
                            + result.getMissingCriticalFields());
                }
            }
            if (!result.getSummary().contains("not enough for a final verdict")) {
                mismatches.add("summary expected concern-first incomplete-verdict language but was <"
                        + result.getSummary() + ">");
            }
            return mismatches;
        }
    }

    private record Failure(int index, CaseSpec spec, OfferTextParseResult result, List<String> mismatches) {

        private List<String> categories() {
            List<String> categories = new ArrayList<>();
            for (String mismatch : mismatches) {
                if (mismatch.startsWith("offerCitySlug")) {
                    categories.add("offer-city");
                } else if (mismatch.startsWith("offerHourlyRate")) {
                    categories.add("offer-hourly-rate");
                } else if (mismatch.startsWith("unitType")) {
                    categories.add("unit-type");
                } else if (mismatch.startsWith("extractedFields")) {
                    categories.add("concern-or-risk-signal");
                } else if (mismatch.startsWith("missingCriticalFields")) {
                    categories.add("missing-critical-fields");
                } else if (mismatch.startsWith("summary")) {
                    categories.add("concern-first-summary");
                } else if (mismatch.startsWith("parsed")) {
                    categories.add("parsed-flag");
                } else {
                    categories.add("other");
                }
            }
            return categories.stream().distinct().toList();
        }

        private String format() {
            return "case " + index + " [" + spec.name() + "]"
                    + "\nINPUT: " + spec.input()
                    + "\nMISMATCHES: " + mismatches
                    + "\nCATEGORIES: " + categories()
                    + "\nEXTRACTED: " + result.getExtractedFields()
                    + "\nMISSING: " + result.getMissingCriticalFields()
                    + "\nSUMMARY: " + result.getSummary();
        }
    }
}
