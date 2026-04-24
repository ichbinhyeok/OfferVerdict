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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OfferTextParserServiceAgent5ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports", "agent5-parser-failures.txt");

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandAgent5CityRateAndUnitAmbiguityCases() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(currentThenOfferCases());
        cases.addAll(offerThenCurrentCases());
        cases.addAll(compactOrderCases());
        cases.addAll(oneCityOnlyCases());
        cases.addAll(multiCityNoiseCases());
        cases.addAll(unitPunctuationCases());

        assertEquals(1000, cases.size(), "Agent 5 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.input(), "offer_review");
            OfferRiskDraft draft = result.getDraft();
            List<String> mismatches = new ArrayList<>();

            expectEquals(mismatches, "currentCitySlug", spec.currentCitySlug(), draft.getCurrentCitySlug());
            expectEquals(mismatches, "offerCitySlug", spec.offerCitySlug(), draft.getOfferCitySlug());
            expectDouble(mismatches, "currentHourlyRate", spec.currentHourlyRate(), draft.getCurrentHourlyRate());
            expectDouble(mismatches, "offerHourlyRate", spec.offerHourlyRate(), draft.getOfferHourlyRate());
            expectEquals(mismatches, "unitType", spec.unitType(), draft.getUnitType());

            for (String missingField : spec.expectedMissingFields()) {
                if (!result.getMissingCriticalFields().contains(missingField)) {
                    mismatches.add("missingCriticalFields expected to contain <" + missingField + "> but was "
                            + result.getMissingCriticalFields());
                }
            }

            if (!mismatches.isEmpty()) {
                failures.add(formatFailure(i, spec, draft, result, mismatches));
            }
        });

        if (!failures.isEmpty()) {
            writeFailureReport(failures);
            fail("Agent 5 adversarial parser failures: " + failures.size()
                    + " of " + cases.size()
                    + ". Full report: " + FAILURE_REPORT.toAbsolutePath()
                    + "\n\n" + String.join("\n\n", failures.stream().limit(40).toList()));
        }
    }

    private List<CaseSpec> currentThenOfferCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 225; i++) {
            City current = currentCities()[i % currentCities().length];
            City offer = offerCities()[i % offerCities().length];
            Unit unit = supportedUnits()[i % supportedUnits().length];
            double currentRate = 40 + (i % 8);
            double offerRate = 56 + (i % 10);
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Current RN job is in %s at $%.0f/hr. New %s offer is in %s at $%.0f/hr.",
                        current.label(), currentRate, unit.label(), offer.label(), offerRate);
                case 1 -> String.format(Locale.US,
                        "I live in %s and make $%.0f/hr now; offer: %s, %s, base $%.0f/hr.",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate);
                case 2 -> String.format(Locale.US,
                        "Currently working %s for $%.0f/hr. Recruiter sent %s RN position located in %s, $%.0f/hr.",
                        current.label(), currentRate, unit.label(), offer.label(), offerRate);
                case 3 -> String.format(Locale.US,
                        "Me now - %s - $%.0f/hr. New role - %s - %s - $%.0f/hr.",
                        current.label(), currentRate, unit.label(), offer.label(), offerRate);
                default -> String.format(Locale.US,
                        "Today I am based in %s making $%.0f/hr. The offer letter says %s unit in %s pays $%.0f/hr.",
                        current.label(), currentRate, unit.label(), offer.label(), offerRate);
            };
            cases.add(new CaseSpec(input, current.slug(), offer.slug(), currentRate, offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> offerThenCurrentCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            City current = currentCities()[(i + 2) % currentCities().length];
            City offer = offerCities()[(i + 1) % offerCities().length];
            Unit unit = supportedUnits()[(i + 3) % supportedUnits().length];
            double currentRate = 41 + (i % 7);
            double offerRate = 58 + (i % 9);
            String input = switch (i % 4) {
                case 0 -> String.format(Locale.US,
                        "Offer first: %s %s RN at $%.0f/hr. My current job is %s at $%.0f/hr.",
                        offer.label(), unit.label(), offerRate, current.label(), currentRate);
                case 1 -> String.format(Locale.US,
                        "New job in %s pays $%.0f/hr for %s. I currently work in %s making $%.0f/hr.",
                        offer.label(), offerRate, unit.label(), current.label(), currentRate);
                case 2 -> String.format(Locale.US,
                        "%s role, %s, base rate $%.0f/hr. Existing job: %s, $%.0f/hr.",
                        unit.label(), offer.label(), offerRate, current.label(), currentRate);
                default -> String.format(Locale.US,
                        "Recruiter offer: %s / %s / $%.0f hourly. Currently: %s / $%.0f hourly.",
                        offer.label(), unit.label(), offerRate, current.label(), currentRate);
            };
            cases.add(new CaseSpec(input, current.slug(), offer.slug(), currentRate, offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> compactOrderCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            City current = currentCities()[(i + 1) % currentCities().length];
            City offer = offerCities()[(i + 4) % offerCities().length];
            Unit unit = allUnits()[i % allUnits().length];
            double currentRate = 39 + (i % 9);
            double offerRate = 57 + (i % 11);
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "RN decision: %s $%.0f/hr -> %s $%.0f/hr, %s.",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
                case 1 -> String.format(Locale.US,
                        "current/%s/$%.0f per hour; offer/%s/$%.0f per hour; unit=%s",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
                case 2 -> String.format(Locale.US,
                        "Me now %s $%.0f/hr. New thing %s $%.0f/hr. %s RN.",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
                case 3 -> String.format(Locale.US,
                        "old: %s at $%.0f/hr | new: %s at $%.0f/hr | %s position",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
                default -> String.format(Locale.US,
                        "%s, $%.0f/hr now. %s, $%.0f/hr offer. %s.",
                        current.label(), currentRate, offer.label(), offerRate, unit.label());
            };
            cases.add(new CaseSpec(input, current.slug(), offer.slug(), currentRate, offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> oneCityOnlyCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City offer = offerCities()[(i + 2) % offerCities().length];
            Unit unit = allUnits()[(i + 5) % allUnits().length];
            double offerRate = 55 + (i % 13);
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "I only have the offer details: %s %s RN, $%.0f/hr. Current pay not handy.",
                        offer.label(), unit.label(), offerRate);
                case 1 -> String.format(Locale.US,
                        "Offer is in %s for a %s role at $%.0f/hr, but I forgot to write my current city.",
                        offer.label(), unit.label(), offerRate);
                case 2 -> String.format(Locale.US,
                        "%s job posting says %s nurse, base rate $%.0f/hr. I am still comparing.",
                        offer.label(), unit.label(), offerRate);
                case 3 -> String.format(Locale.US,
                        "New role %s: %s, $%.0f/hr. No current info yet.",
                        offer.label(), unit.label(), offerRate);
                default -> String.format(Locale.US,
                        "Got a %s offer, located in %s, paying $%.0f/hr.",
                        unit.label(), offer.label(), offerRate);
            };
            cases.add(new CaseSpec(input, null, offer.slug(), null, offerRate, unit.slug(), "Current city", "Current hourly rate"));
        }
        return cases;
    }

    private List<CaseSpec> multiCityNoiseCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] noiseCities = {
                new City("Boston, MA", "boston-ma"),
                new City("Chicago, IL", "chicago-il"),
                new City("Houston, TX", "houston-tx"),
                new City("Philadelphia, PA", "philadelphia-pa")
        };
        for (int i = 0; i < 125; i++) {
            City current = currentCities()[(i + 3) % currentCities().length];
            City offer = offerCities()[(i + 5) % offerCities().length];
            City noise = noiseCities[i % noiseCities.length];
            Unit unit = supportedUnits()[(i + 1) % supportedUnits().length];
            double currentRate = 42 + (i % 6);
            double offerRate = 59 + (i % 8);
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "I work in %s at $%.0f/hr. Offer is %s %s at $%.0f/hr. Family keeps mentioning %s but that is not the job.",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate, noise.label());
                case 1 -> String.format(Locale.US,
                        "Current: %s $%.0f/hr. Offer: %s $%.0f/hr %s RN. Recruiter also compared housing to %s.",
                        current.label(), currentRate, offer.label(), offerRate, unit.label(), noise.label());
                case 2 -> String.format(Locale.US,
                        "Not relocating to %s. My current job is in %s making $%.0f/hr; the offer city is %s at $%.0f/hr for %s.",
                        noise.label(), current.label(), currentRate, offer.label(), offerRate, unit.label());
                case 3 -> String.format(Locale.US,
                        "Considering visits to %s, but the actual offer is %s %s $%.0f/hr. Current is %s $%.0f/hr.",
                        noise.label(), offer.label(), unit.label(), offerRate, current.label(), currentRate);
                default -> String.format(Locale.US,
                        "I live near %s sometimes, currently %s $%.0f/hr, offer is located in %s for %s at $%.0f/hr.",
                        noise.label(), current.label(), currentRate, offer.label(), unit.label(), offerRate);
            };
            cases.add(new CaseSpec(input, current.slug(), offer.slug(), currentRate, offerRate, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> unitPunctuationCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] unitLabels = {
                "ICU,", "ED.", "OR;", "L&D:", "clinic/", "float pool,", "med-surg.", "ms/tele:"
        };
        String[] unitSlugs = {
                "icu", "ed", "or", "l_and_d", "clinic", "float_pool", "med_surg", "med_surg"
        };
        for (int i = 0; i < 150; i++) {
            City current = currentCities()[(i + 4) % currentCities().length];
            City offer = offerCities()[(i + 2) % offerCities().length];
            int unitIndex = i % unitLabels.length;
            double currentRate = 43 + (i % 5);
            double offerRate = 60 + (i % 7);
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Current %s $%.0f/hr. Offer %s $%.0f/hr. Unit: %s RN.",
                        current.label(), currentRate, offer.label(), offerRate, unitLabels[unitIndex]);
                case 1 -> String.format(Locale.US,
                        "I am in %s making $%.0f/hr; new %s nurse role in %s pays $%.0f/hr.",
                        current.label(), currentRate, unitLabels[unitIndex], offer.label(), offerRate);
                case 2 -> String.format(Locale.US,
                        "Current city=%s rate=$%.0f/hr. Offer city=%s rate=$%.0f/hr. Dept=%s",
                        current.label(), currentRate, offer.label(), offerRate, unitLabels[unitIndex]);
                case 3 -> String.format(Locale.US,
                        "Old RN job: %s, $%.0f/hr. New RN job: %s, $%.0f/hr, %s position.",
                        current.label(), currentRate, offer.label(), offerRate, unitLabels[unitIndex]);
                default -> String.format(Locale.US,
                        "%s at $%.0f/hr now; %s at $%.0f/hr offer; %s shift.",
                        current.label(), currentRate, offer.label(), offerRate, unitLabels[unitIndex]);
            };
            cases.add(new CaseSpec(input, current.slug(), offer.slug(), currentRate, offerRate, unitSlugs[unitIndex]));
        }
        return cases;
    }

    private static City[] currentCities() {
        return new City[] {
                new City("Austin, TX", "austin-tx"),
                new City("Phoenix, AZ", "phoenix-az"),
                new City("Chicago, IL", "chicago-il"),
                new City("Houston, TX", "houston-tx"),
                new City("Miami, FL", "miami-fl")
        };
    }

    private static City[] offerCities() {
        return new City[] {
                new City("Seattle, WA", "seattle-wa"),
                new City("Los Angeles, CA", "los-angeles-ca"),
                new City("LA", "los-angeles-ca"),
                new City("New York City", "new-york-ny"),
                new City("NYC", "new-york-ny"),
                new City("Boston, MA", "boston-ma"),
                new City("Philadelphia, PA", "philadelphia-pa")
        };
    }

    private static Unit[] supportedUnits() {
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

    private static Unit[] allUnits() {
        return new Unit[] {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("OR", "or"),
                new Unit("L&D", "l_and_d"),
                new Unit("clinic", "clinic"),
                new Unit("float pool", "float_pool"),
                new Unit("med surg tele", "med_surg"),
                new Unit("ms/tele", "med_surg")
        };
    }

    private static void expectEquals(List<String> mismatches, String field, String expected, String actual) {
        if (expected == null) {
            return;
        }
        if (!expected.equals(actual)) {
            mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void expectDouble(List<String> mismatches, String field, Double expected, double actual) {
        if (expected == null) {
            return;
        }
        if (Math.abs(expected - actual) > 0.01) {
            mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static String formatFailure(int index,
                                        CaseSpec spec,
                                        OfferRiskDraft draft,
                                        OfferTextParseResult result,
                                        List<String> mismatches) {
        return "case " + index
                + "\nINPUT: " + spec.input()
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
                + "\nMISSING: " + result.getMissingCriticalFields();
    }

    private static void writeFailureReport(List<String> failures) {
        try {
            Files.createDirectories(FAILURE_REPORT.getParent());
            Files.writeString(FAILURE_REPORT, String.join("\n\n", failures));
        } catch (IOException ignored) {
            // The assertion message still contains the first failures if report writing is unavailable.
        }
    }

    private record City(String label, String slug) {
    }

    private record Unit(String label, String slug) {
    }

    private record CaseSpec(String input,
                            String currentCitySlug,
                            String offerCitySlug,
                            Double currentHourlyRate,
                            Double offerHourlyRate,
                            String unitType,
                            List<String> expectedMissingFields) {

        private CaseSpec(String input,
                         String currentCitySlug,
                         String offerCitySlug,
                         Double currentHourlyRate,
                         Double offerHourlyRate,
                         String unitType,
                         String... expectedMissingFields) {
            this(input, currentCitySlug, offerCitySlug, currentHourlyRate, offerHourlyRate, unitType,
                    List.of(expectedMissingFields));
        }
    }
}
