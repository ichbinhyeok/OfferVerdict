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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class OfferTextParserServiceAgent6ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports", "agent6-parser-scale-failures.txt");

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandAgent6MessyNurseOfferTexts() throws IOException {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(lowPunctuationCases());
        cases.addAll(separatorRemovedCases());
        cases.addAll(abbreviationAndMissingCommaCases());
        cases.addAll(cityRateOrderFlipCases());
        cases.addAll(offerFirstCurrentLaterCases());

        assertEquals(1000, cases.size(), "agent6 corpus size drifted");

        List<Failure> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(index -> {
            CaseSpec spec = cases.get(index);
            OfferTextParseResult result = parser.parse(spec.text(), "offer_review");
            List<String> mismatches = spec.mismatches(result);
            if (!mismatches.isEmpty()) {
                failures.add(new Failure(index, spec, result, mismatches));
            }
        });

        writeReport(cases.size(), failures);
        if (!failures.isEmpty()) {
            fail("Agent 6 messy nurse offer parser failures: " + failures.size()
                    + " of " + cases.size()
                    + ". Unique failure categories: " + categoryCounts(failures)
                    + ". Full report: " + FAILURE_REPORT.toAbsolutePath()
                    + "\n\n" + String.join("\n\n", failures.stream().limit(30).map(Failure::format).toList()));
        }
    }

    private List<CaseSpec> lowPunctuationCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City current = currentCities()[i % currentCities().length];
            City offer = offerCities()[(i + 2) % offerCities().length];
            Unit unit = units()[i % units().length];
            double currentRate = 39 + (i % 10);
            double offerRate = 56 + (i % 12);
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "current rn %s making $%.0f/hr new offer %s %s rn $%.0f/hr 36 hrs nights",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
                case 1 -> String.format(Locale.US,
                        "i work in %s $%.0f/hr now got offer in %s for %s $%.0f/hr 36h",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
                case 2 -> String.format(Locale.US,
                        "me now %s $%.0f/hr offer city %s unit %s base rate $%.0f/hr",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
                case 3 -> String.format(Locale.US,
                        "old job %s rn $%.0f/hr new rn job %s %s $%.0f/hr",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
                default -> String.format(Locale.US,
                        "currently based in %s making $%.0f per hour offer located in %s %s pays $%.0f per hour",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
            };
            cases.add(new CaseSpec("low-punctuation-" + i, text, current.slug(), offer.slug(), currentRate,
                    offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> separatorRemovedCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City current = currentCities()[(i + 1) % currentCities().length];
            City offer = offerCities()[(i + 3) % offerCities().length];
            Unit unit = units()[(i + 2) % units().length];
            double currentRate = 41 + (i % 8);
            double offerRate = 58 + (i % 10);
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "current %s $%.0f/hr offer %s %s $%.0f/hr sign on $%d relo $%d",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate, 9000 + i,
                        3000 + (i % 5) * 500);
                case 1 -> String.format(Locale.US,
                        "now %s rn $%.0f/hr new role %s rn %s base rate $%.0f/hr 3x12",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
                case 2 -> String.format(Locale.US,
                        "current city %s current pay $%.0f/hr offer city %s offer rate $%.0f/hr dept %s",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
                case 3 -> String.format(Locale.US,
                        "working in %s making $%.0f/hr recruiter offer located in %s %s nurse $%.0f/hr",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
                default -> String.format(Locale.US,
                        "my current %s $%.0f hourly new job %s %s $%.0f hourly",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
            };
            cases.add(new CaseSpec("separator-removed-" + i, text, current.slug(), offer.slug(), currentRate,
                    offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> abbreviationAndMissingCommaCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City current = currentCities()[(i + 2) % currentCities().length];
            City offer = offerCities()[(i + 4) % offerCities().length];
            Unit unit = abbreviationUnits()[i % abbreviationUnits().length];
            double currentRate = 40 + (i % 9);
            double offerRate = 57 + (i % 11);
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "RN now %s $%.0f/hr offer %s %s $%.0f/hr nocs 36h",
                        current.noCommaLabel(), currentRate, offer.noCommaLabel(), unit.label(), offerRate);
                case 1 -> String.format(Locale.US,
                        "cur %s $%.0f/hr new %s RN in %s $%.0f/hr",
                        current.noCommaLabel(), currentRate, unit.label(), offer.noCommaLabel(), offerRate);
                case 2 -> String.format(Locale.US,
                        "current job %s rate $%.0f per hour offer %s %s rate $%.0f per hour",
                        current.noCommaLabel(), currentRate, offer.noCommaLabel(), unit.label(), offerRate);
                case 3 -> String.format(Locale.US,
                        "currently %s rn %.0f hourly offered %s %s %.0f hourly",
                        current.noCommaLabel(), currentRate, offer.noCommaLabel(), unit.label(), offerRate);
                default -> String.format(Locale.US,
                        "existing job %s $%.0f/hr position located in %s %s $%.0f/hr",
                        current.noCommaLabel(), currentRate, offer.noCommaLabel(), unit.label(), offerRate);
            };
            cases.add(new CaseSpec("abbrev-missing-comma-" + i, text, current.slug(), offer.slug(), currentRate,
                    offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> cityRateOrderFlipCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City current = currentCities()[(i + 3) % currentCities().length];
            City offer = offerCities()[(i + 1) % offerCities().length];
            Unit unit = units()[(i + 1) % units().length];
            double currentRate = 42 + (i % 7);
            double offerRate = 59 + (i % 9);
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "$%.0f/hr current in %s $%.0f/hr offer in %s %s",
                        currentRate, current.label(), offerRate, offer.label(), unit.label());
                case 1 -> String.format(Locale.US,
                        "%s is current $%.0f/hr %s is offer $%.0f/hr %s RN",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
                case 2 -> String.format(Locale.US,
                        "rate now $%.0f/hr city now %s rate offer $%.0f/hr city offer %s unit %s",
                        currentRate, current.label(), offerRate, offer.label(), unit.label());
                case 3 -> String.format(Locale.US,
                        "current pay $%.0f/hr current city %s offer pay $%.0f/hr offer city %s dept %s",
                        currentRate, current.label(), offerRate, offer.label(), unit.label());
                default -> String.format(Locale.US,
                        "%s $%.0f/hr today then %s $%.0f/hr new role %s",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
            };
            cases.add(new CaseSpec("city-rate-flip-" + i, text, current.slug(), offer.slug(), currentRate,
                    offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> offerFirstCurrentLaterCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City current = currentCities()[(i + 4) % currentCities().length];
            City offer = offerCities()[i % offerCities().length];
            Unit unit = units()[(i + 3) % units().length];
            double currentRate = 43 + (i % 6);
            double offerRate = 60 + (i % 8);
            String text = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "offer first %s %s rn base rate $%.0f/hr 36 hrs current job later %s $%.0f/hr",
                        offer.label(), unit.label(), offerRate, current.label(), currentRate);
                case 1 -> String.format(Locale.US,
                        "new role %s %s $%.0f/hr nights my current is %s making $%.0f/hr",
                        offer.label(), unit.label(), offerRate, current.label(), currentRate);
                case 2 -> String.format(Locale.US,
                        "recruiter offer %s $%.0f/hr %s nurse currently in %s at $%.0f/hr",
                        offer.label(), offerRate, unit.label(), current.label(), currentRate);
                case 3 -> String.format(Locale.US,
                        "offered %s %s hourly $%.0f/hr coming from %s hourly $%.0f/hr now",
                        offer.label(), unit.label(), offerRate, current.label(), currentRate);
                default -> String.format(Locale.US,
                        "offer city %s offer rate $%.0f/hr unit %s current city %s current rate $%.0f/hr",
                        offer.label(), offerRate, unit.label(), current.label(), currentRate);
            };
            cases.add(new CaseSpec("offer-first-current-later-" + i, text, current.slug(), offer.slug(), currentRate,
                    offerRate, unit.slug()));
        }
        return cases;
    }

    private static City[] currentCities() {
        return new City[] {
                new City("Austin, TX", "Austin TX", "austin-tx"),
                new City("Phoenix, AZ", "Phoenix AZ", "phoenix-az"),
                new City("Chicago, IL", "Chicago IL", "chicago-il"),
                new City("Houston, TX", "Houston TX", "houston-tx"),
                new City("Miami, FL", "Miami FL", "miami-fl")
        };
    }

    private static City[] offerCities() {
        return new City[] {
                new City("Seattle, WA", "Seattle WA", "seattle-wa"),
                new City("Los Angeles, CA", "Los Angeles CA", "los-angeles-ca"),
                new City("LA", "LA", "los-angeles-ca"),
                new City("New York City", "New York City", "new-york-ny"),
                new City("NYC", "NYC", "new-york-ny"),
                new City("Boston, MA", "Boston MA", "boston-ma"),
                new City("Philadelphia, PA", "Philadelphia PA", "philadelphia-pa")
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

    private static Unit[] abbreviationUnits() {
        return new Unit[] {
                new Unit("ICU", "icu"),
                new Unit("ER", "ed"),
                new Unit("OR", "or"),
                new Unit("L&D", "l_and_d"),
                new Unit("clinic", "clinic"),
                new Unit("float", "float_pool"),
                new Unit("ms tele", "med_surg")
        };
    }

    private static void writeReport(int total, List<Failure> failures) throws IOException {
        Files.createDirectories(FAILURE_REPORT.getParent());
        String status = "PASS: " + (total - failures.size()) + "\n"
                + "FAIL: " + failures.size() + "\n"
                + "TOTAL: " + total + "\n"
                + "UNIQUE_FAILURE_CATEGORIES: " + categoryCounts(failures) + "\n\n";
        Files.writeString(FAILURE_REPORT, status
                + String.join("\n\n", failures.stream().map(Failure::format).toList()));
    }

    private static Map<String, Integer> categoryCounts(List<Failure> failures) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Failure failure : failures) {
            for (String mismatch : failure.mismatches()) {
                String category = mismatch.substring(0, mismatch.indexOf(' '));
                counts.merge(category, 1, Integer::sum);
            }
        }
        return counts;
    }

    private record City(String label, String noCommaLabel, String slug) {
    }

    private record Unit(String label, String slug) {
    }

    private record CaseSpec(String name,
                            String text,
                            String currentCitySlug,
                            String offerCitySlug,
                            double currentHourlyRate,
                            double offerHourlyRate,
                            String unitType) {

        private List<String> mismatches(OfferTextParseResult result) {
            OfferRiskDraft draft = result.getDraft();
            List<String> mismatches = new ArrayList<>();
            requireEquals(mismatches, "currentCitySlug", currentCitySlug, draft.getCurrentCitySlug());
            requireEquals(mismatches, "offerCitySlug", offerCitySlug, draft.getOfferCitySlug());
            requireClose(mismatches, "currentHourlyRate", currentHourlyRate, draft.getCurrentHourlyRate());
            requireClose(mismatches, "offerHourlyRate", offerHourlyRate, draft.getOfferHourlyRate());
            requireEquals(mismatches, "unitType", unitType, draft.getUnitType());
            return mismatches;
        }

        private static void requireEquals(List<String> mismatches, String field, String expected, String actual) {
            if (!expected.equals(actual)) {
                mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
            }
        }

        private static void requireClose(List<String> mismatches, String field, double expected, double actual) {
            if (Math.abs(expected - actual) > 0.01) {
                mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
            }
        }
    }

    private record Failure(int index, CaseSpec spec, OfferTextParseResult result, List<String> mismatches) {

        private String format() {
            OfferRiskDraft draft = result.getDraft();
            return "CASE " + index + " [" + spec.name() + "]"
                    + "\nINPUT: " + spec.text()
                    + "\nEXPECTED: currentCitySlug=" + spec.currentCitySlug()
                    + ", offerCitySlug=" + spec.offerCitySlug()
                    + ", currentHourlyRate=" + spec.currentHourlyRate()
                    + ", offerHourlyRate=" + spec.offerHourlyRate()
                    + ", unitType=" + spec.unitType()
                    + "\nACTUAL: currentCitySlug=" + draft.getCurrentCitySlug()
                    + ", offerCitySlug=" + draft.getOfferCitySlug()
                    + ", currentHourlyRate=" + draft.getCurrentHourlyRate()
                    + ", offerHourlyRate=" + draft.getOfferHourlyRate()
                    + ", unitType=" + draft.getUnitType()
                    + "\nMISMATCHES: " + mismatches
                    + "\nEXTRACTED: " + result.getExtractedFields()
                    + "\nMISSING: " + result.getMissingCriticalFields()
                    + "\nWARNING: " + result.getParseWarning();
        }
    }
}
