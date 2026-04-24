package com.offerverdict.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.CityCostEntry;
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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class OfferTextParserServiceAgent10ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports", "agent10-parser-failures.txt");

    private final DataRepository repository = repository();
    private final OfferTextParserService parser = new OfferTextParserService(repository);

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandAgent10CityRateUnitAmbiguityCases() {
        List<CaseSpec> cases = cases();

        assertEquals(1000, cases.size(), "Agent 10 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) {
        List<Failure> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.input(), "offer_review");
            OfferRiskDraft draft = result.getDraft();
            List<String> mismatches = mismatches(spec, draft);

            if (!mismatches.isEmpty()) {
                failures.add(new Failure(i, spec, draft, result, mismatches));
            }
        });

        if (!failures.isEmpty()) {
            writeFailureReport(failures);
            fail("Agent 10 adversarial parser failures: " + failures.size()
                    + " of " + cases.size()
                    + "; categories=" + categoryCounts(failures)
                    + ". Full report: " + FAILURE_REPORT.toAbsolutePath()
                    + "\n\n" + previewFailures(failures));
        }
    }

    private List<CaseSpec> cases() {
        List<CaseSpec> cases = new ArrayList<>();
        List<City> cities = cities();
        Unit[] units = units();

        for (int i = 0; i < 1000; i++) {
            City current = cities.get(i % cities.size());
            City offer = cities.get((i * 7 + 11) % cities.size());
            if (current.slug().equals(offer.slug())) {
                offer = cities.get((i * 7 + 12) % cities.size());
            }
            City noise = cities.get((i * 13 + 5) % cities.size());
            if (noise.slug().equals(current.slug()) || noise.slug().equals(offer.slug())) {
                noise = cities.get((i * 13 + 6) % cities.size());
            }
            Unit unit = units[i % units.length];
            double currentRate = 38 + (i % 17);
            double offerRate = 55 + (i % 19);
            String category = category(i);

            cases.add(new CaseSpec(
                    category,
                    inputFor(category, current, offer, noise, unit, currentRate, offerRate, i),
                    current.slug(),
                    offer.slug(),
                    currentRate,
                    offerRate,
                    unit.slug()));
        }

        return cases;
    }

    private String inputFor(String category,
                            City current,
                            City offer,
                            City noise,
                            Unit unit,
                            double currentRate,
                            double offerRate,
                            int index) {
        return switch (category) {
            case "city-rate-unit-ambiguity" -> String.format(Locale.US,
                    "Current city %s, current hourly $%.0f/hr. Offer city %s, offer hourly $%.0f/hr. Unit %s RN; 36 hrs/wk.",
                    current.label(), currentRate, offer.label(), offerRate, unit.label());
            case "repeated-city-mentions" -> String.format(Locale.US,
                    "I keep saying %s because the recruiter said %s twice, but my current city is %s and current pay is $%.0f/hr. The actual offer city is %s at $%.0f/hr for %s RN.",
                    offer.label(), offer.label(), current.label(), currentRate, offer.label(), offerRate, unit.label());
            case "near-current-offer-phrasing" -> String.format(Locale.US,
                    "I am currently near %s, current city %s, making $%.0f/hr. The near-term offer is in %s for a %s registered nurse at $%.0f/hr.",
                    noise.label(), current.label(), currentRate, offer.label(), unit.label(), offerRate);
            case "arrow-transition" -> String.format(Locale.US,
                    "RN move: current %s $%.0f/hr -> offer %s $%.0f/hr -> %s unit, guaranteed 36h.",
                    current.label(), currentRate, offer.label(), offerRate, unit.label());
            case "old-new-phrasing" -> String.format(Locale.US,
                    "Old job: %s RN at $%.0f/hr. New job: %s %s RN at $%.0f/hr. Ignore %s; that was a comparison city.",
                    current.label(), currentRate, offer.label(), unit.label(), offerRate, noise.label());
            case "current-offer-hourly-separation" -> String.format(Locale.US,
                    "Current hourly rate is $%.0f/hr and current city is %s. Offer hourly rate is $%.0f/hr and offer city is %s. Department abbreviation: %s RN.",
                    currentRate, current.label(), offerRate, offer.label(), unit.label());
            case "unit-abbreviations" -> String.format(Locale.US,
                    "Currently in %s making $%.0f/hr; offered %s role in %s at $%.0f/hr. Unit code from text message: %s.",
                    current.label(), currentRate, unit.label(), offer.label(), offerRate, unit.label());
            default -> String.format(Locale.US,
                    "Compare city/rate pairs: current=%s pay=$%.0f/hr; offer=%s pay=$%.0f/hr. Mentioned %s in passing only. %s RN, 36 hours.",
                    current.label(), currentRate, offer.label(), offerRate, noise.label(), unit.label());
        };
    }

    private String category(int index) {
        return switch (index % 8) {
            case 0 -> "city-rate-unit-ambiguity";
            case 1 -> "repeated-city-mentions";
            case 2 -> "near-current-offer-phrasing";
            case 3 -> "arrow-transition";
            case 4 -> "old-new-phrasing";
            case 5 -> "current-offer-hourly-separation";
            case 6 -> "unit-abbreviations";
            default -> "supported-city-noise";
        };
    }

    private List<City> cities() {
        return repository.getCities().stream()
                .map(city -> new City(label(city), city.getSlug()))
                .toList();
    }

    private String label(CityCostEntry city) {
        return city.getCity() + ", " + city.getState();
    }

    private Unit[] units() {
        return new Unit[] {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("OR", "or"),
                new Unit("L&D", "l_and_d"),
                new Unit("clinic", "clinic"),
                new Unit("float pool", "float_pool"),
                new Unit("med-surg", "med_surg"),
                new Unit("ms/tele", "med_surg")
        };
    }

    private List<String> mismatches(CaseSpec spec, OfferRiskDraft draft) {
        List<String> mismatches = new ArrayList<>();
        expectEquals(mismatches, "currentCitySlug", spec.currentCitySlug(), draft.getCurrentCitySlug());
        expectEquals(mismatches, "offerCitySlug", spec.offerCitySlug(), draft.getOfferCitySlug());
        expectDouble(mismatches, "currentHourlyRate", spec.currentHourlyRate(), draft.getCurrentHourlyRate());
        expectDouble(mismatches, "offerHourlyRate", spec.offerHourlyRate(), draft.getOfferHourlyRate());
        expectEquals(mismatches, "unitType", spec.unitType(), draft.getUnitType());
        return mismatches;
    }

    private static void expectEquals(List<String> mismatches, String field, String expected, String actual) {
        if (!expected.equals(actual)) {
            mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void expectDouble(List<String> mismatches, String field, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.01) {
            mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static Map<String, Long> categoryCounts(List<Failure> failures) {
        Map<String, Long> counts = new LinkedHashMap<>();
        failures.stream()
                .map(failure -> failure.spec().category())
                .sorted()
                .forEach(category -> counts.merge(category, 1L, Long::sum));
        return counts;
    }

    private static String previewFailures(List<Failure> failures) {
        return String.join("\n\n", failures.stream()
                .limit(40)
                .map(OfferTextParserServiceAgent10ScaleTest::formatFailure)
                .toList());
    }

    private static String formatFailure(Failure failure) {
        OfferRiskDraft draft = failure.draft();
        OfferTextParseResult result = failure.result();
        CaseSpec spec = failure.spec();
        return "case " + failure.index()
                + " category=" + spec.category()
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
                + "\nMISMATCHES: " + failure.mismatches()
                + "\nEXTRACTED: " + result.getExtractedFields()
                + "\nMISSING: " + result.getMissingCriticalFields()
                + "\nSUMMARY: " + result.getSummary();
    }

    private static void writeFailureReport(List<Failure> failures) {
        List<String> lines = new ArrayList<>();
        lines.add("Agent 10 parser failures: " + failures.size());
        lines.add("Category counts: " + categoryCounts(failures));
        lines.add("");
        failures.stream()
                .map(OfferTextParserServiceAgent10ScaleTest::formatFailure)
                .forEach(lines::add);
        try {
            Files.createDirectories(FAILURE_REPORT.getParent());
            Files.writeString(FAILURE_REPORT, String.join("\n\n", lines));
        } catch (IOException ignored) {
            // The assertion message still includes a bounded preview if the report cannot be written.
        }
    }

    private record City(String label, String slug) {
    }

    private record Unit(String label, String slug) {
    }

    private record CaseSpec(String category,
                            String input,
                            String currentCitySlug,
                            String offerCitySlug,
                            double currentHourlyRate,
                            double offerHourlyRate,
                            String unitType) {
    }

    private record Failure(int index,
                           CaseSpec spec,
                           OfferRiskDraft draft,
                           OfferTextParseResult result,
                           List<String> mismatches) {
    }
}
