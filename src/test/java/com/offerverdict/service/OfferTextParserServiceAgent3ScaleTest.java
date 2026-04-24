package com.offerverdict.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.OfferTextParseResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferTextParserServiceAgent3ScaleTest {

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandLowInformationConcernInputsWithoutPrematureVerdicts() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(toxicCultureAndBullyingCases());
        cases.addAll(unsafeRatioAndStaffingCases());
        cases.addAll(floatAnxietyCases());
        cases.addAll(payCutCases());
        cases.addAll(goodCoworkerTradeoffCases());
        cases.addAll(childcareCommuteFamilyCases());
        cases.addAll(unknownCityAndPayCases());
        cases.addAll(partialCityOnlyCases());
        cases.addAll(partialMoneyOnlyCases());
        cases.addAll(realWorldUnsupportedPhraseCases());

        assertEquals(1000, cases.size(), "agent 3 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.text(), "offer_review");
            List<String> problems = spec.problems(result);
            if (!problems.isEmpty()) {
                failures.add("case " + i + " [" + spec.name() + "]"
                        + "\nINPUT: " + spec.text()
                        + "\nEXPECTED: " + String.join("; ", spec.expectations())
                        + "\nACTUAL_EXTRACTED: " + result.getExtractedFields()
                        + "\nACTUAL_MISSING: " + result.getMissingCriticalFields()
                        + "\nACTUAL_SUMMARY: " + result.getSummary()
                        + "\nACTUAL_WARNING: " + result.getParseWarning()
                        + "\nPROBLEMS: " + String.join("; ", problems));
            }
        });

        writeFailureReport(failures);
        assertTrue(failures.isEmpty(), String.join("\n\n", failures));
    }

    private void writeFailureReport(List<String> failures) {
        try {
            Path report = Path.of("build", "reports", "agent3-parser-scale-failures.txt");
            Files.createDirectories(report.getParent());
            String body = failures.isEmpty()
                    ? "PASS: 1000 agent 3 low-information concern cases passed.\n"
                    : String.join("\n\n", failures);
            Files.writeString(report, body, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new AssertionError("Could not write agent 3 failure report", error);
        }
    }

    private List<CaseSpec> toxicCultureAndBullyingCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] phrases = {
                "toxic culture",
                "bullying",
                "hostile culture",
                "unsafe culture",
                "lateral violence",
                "nurses eat their young",
                "bad culture",
                "taeum"
        };
        for (int i = 0; i < 120; i++) {
            String phrase = phrases[i % phrases.length];
            String text = String.format(Locale.US,
                    "I got an RN offer and heard the unit has %s. I am anxious about signing before I know the pay.",
                    phrase);
            cases.add(new CaseSpec("toxic-culture-" + i, text,
                    Set.of("Concern: unit culture / bullying risk"),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> unsafeRatioAndStaffingCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] phrases = {
                "short staffed",
                "short-staffed",
                "unsafe ratios",
                "staffing ratio",
                "patient load",
                "no breaks",
                "no lunch",
                "too many patients"
        };
        for (int i = 0; i < 120; i++) {
            String phrase = phrases[i % phrases.length];
            String text = String.format(Locale.US,
                    "The recruiter says it is an ICU nurse offer, but people keep mentioning %s. I do not have the hourly rate or city confirmed.",
                    phrase);
            cases.add(new CaseSpec("unsafe-staffing-" + i, text,
                    Set.of("Concern: staffing / survivability risk"),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> floatAnxietyCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] phrases = {
                "hospital-wide float",
                "float to any unit",
                "across the hospital",
                "throughout the hospital"
        };
        for (int i = 0; i < 100; i++) {
            String phrase = phrases[i % phrases.length];
            String text = String.format(Locale.US,
                    "I got an RN offer and the thing stressing me out is %s. I only have a recruiter text, no pay details yet.",
                    phrase);
            cases.add(new CaseSpec("float-anxiety-" + i, text,
                    Set.of("Float terms: Hospital-wide float"),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    false));
        }
        return cases;
    }

    private List<CaseSpec> payCutCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] phrases = {
                "pay cut",
                "lower pay",
                "less money",
                "income went down",
                "income is lower",
                "take-home is lower",
                "take home is lower",
                "net pay is lower",
                "making less",
                "pays less"
        };
        for (int i = 0; i < 110; i++) {
            String phrase = phrases[i % phrases.length];
            String text = String.format(Locale.US,
                    "I got an offer but it feels like a %s after rent and benefits. I do not know if I should stay.",
                    phrase);
            cases.add(new CaseSpec("pay-cut-" + i, text,
                    Set.of("Concern: lower take-home pay"),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> goodCoworkerTradeoffCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] phrases = {
                "good coworkers",
                "good co-workers",
                "like my coworkers",
                "like my co-workers",
                "team seems good",
                "team is good",
                "coworkers are good",
                "co-workers are good",
                "manager seems good",
                "good manager",
                "good preceptor",
                "supportive team"
        };
        for (int i = 0; i < 110; i++) {
            String phrase = phrases[i % phrases.length];
            String text = String.format(Locale.US,
                    "I have an RN offer and the %s, but I am not sure the numbers work.",
                    phrase);
            cases.add(new CaseSpec("good-team-tradeoff-" + i, text,
                    Set.of("Positive tradeoff: team / support seems strong"),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> childcareCommuteFamilyCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] phrases = {
                "closer to family",
                "near family",
                "better commute",
                "shorter commute",
                "school schedule",
                "childcare",
                "daycare"
        };
        for (int i = 0; i < 100; i++) {
            String phrase = phrases[i % phrases.length];
            String text = String.format(Locale.US,
                    "The RN offer might help because of %s, but I only know the vibes and not the pay.",
                    phrase);
            cases.add(new CaseSpec("life-fit-" + i, text,
                    Set.of("Personal tradeoff: lifestyle or family fit matters"),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> unknownCityAndPayCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] concerns = {
                "toxic culture",
                "unsafe ratios",
                "pay cut",
                "childcare",
                "good coworkers"
        };
        String[] expected = {
                "Concern: unit culture / bullying risk",
                "Concern: staffing / survivability risk",
                "Concern: lower take-home pay",
                "Personal tradeoff: lifestyle or family fit matters",
                "Positive tradeoff: team / support seems strong"
        };
        for (int i = 0; i < 100; i++) {
            int index = i % concerns.length;
            String text = String.format(Locale.US,
                    "I got an offer but the only thing I know is %s is a concern. No city, no hourly, no current comparison yet.",
                    concerns[index]);
            cases.add(new CaseSpec("unknown-city-pay-" + i, text,
                    Set.of(expected[index]),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> partialCityOnlyCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] cities = {
                new City("Seattle, WA", "Offer city: Seattle, WA"),
                new City("Los Angeles, CA", "Offer city: Los Angeles, CA"),
                new City("New York City", "Offer city: New York, NY"),
                new City("Phoenix, AZ", "Offer city: Phoenix, AZ")
        };
        String[] concerns = {
                "toxic culture",
                "unsafe ratios",
                "income is lower",
                "supportive team",
                "childcare"
        };
        String[] expected = {
                "Concern: unit culture / bullying risk",
                "Concern: staffing / survivability risk",
                "Concern: lower take-home pay",
                "Positive tradeoff: team / support seems strong",
                "Personal tradeoff: lifestyle or family fit matters"
        };
        for (int i = 0; i < 100; i++) {
            City city = cities[i % cities.length];
            int concernIndex = i % concerns.length;
            String text = String.format(Locale.US,
                    "I got an RN offer in %s. The concern is %s, but I do not have the hourly rate yet.",
                    city.label(), concerns[concernIndex]);
            cases.add(new CaseSpec("partial-city-" + i, text,
                    Set.of(city.expectedField(), expected[concernIndex]),
                    Set.of("Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> partialMoneyOnlyCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] money = {
                "$15,000 sign-on bonus",
                "$4,000 relocation stipend",
                "$12k bonus",
                "$5k relo",
                "$250 insurance premium"
        };
        String[] concerns = {
                "toxic culture",
                "short staffed",
                "less money",
                "good manager",
                "near family"
        };
        String[] expected = {
                "Concern: unit culture / bullying risk",
                "Concern: staffing / survivability risk",
                "Concern: lower take-home pay",
                "Positive tradeoff: team / support seems strong",
                "Personal tradeoff: lifestyle or family fit matters"
        };
        for (int i = 0; i < 100; i++) {
            int index = i % money.length;
            String text = String.format(Locale.US,
                    "The only concrete number I have is %s. I am worried about %s and have no city or hourly rate.",
                    money[index], concerns[index]);
            cases.add(new CaseSpec("partial-money-" + i, text,
                    Set.of(expected[index]),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private List<CaseSpec> realWorldUnsupportedPhraseCases() {
        List<CaseSpec> cases = new ArrayList<>();
        String[] texts = {
                "I got an RN offer but the unit sounds mean-girl and cliquey. No pay or city details yet.",
                "The ratios sound sketchy and everyone says the floor is a dumpster fire. I do not know the hourly.",
                "I might make less after benefits, but the team passes the vibe check. No current city or offer city yet.",
                "The commute will wreck my kid pickup schedule, but the manager seems normal. No hourly rate yet.",
                "I am scared they will float me everywhere and cancel me first when census drops. No written offer yet."
        };
        String[][] expectedSignals = {
                {"Concern: unit culture / bullying risk"},
                {"Concern: staffing / survivability risk"},
                {"Concern: lower take-home pay", "Positive tradeoff: team / support seems strong"},
                {"Personal tradeoff: lifestyle or family fit matters", "Positive tradeoff: team / support seems strong"},
                {"Float terms: Hospital-wide float", "Cancellation terms: Low-census cancellation possible"}
        };
        for (int i = 0; i < 40; i++) {
            int index = i % texts.length;
            cases.add(new CaseSpec("real-world-unsupported-" + i, texts[index],
                    Set.of(expectedSignals[index]),
                    Set.of("Offer city", "Offer hourly rate", "Current city", "Current hourly rate"),
                    true));
        }
        return cases;
    }

    private record CaseSpec(String name,
                            String text,
                            Set<String> expectedExtractedFields,
                            Set<String> expectedMissingFields,
                            boolean expectConcernGate) {
        private List<String> expectations() {
            List<String> expectations = new ArrayList<>();
            expectations.add("missing basics: " + expectedMissingFields);
            expectations.add("extract signals: " + expectedExtractedFields);
            expectations.add(expectConcernGate
                    ? "summary warns not enough for a final verdict"
                    : "summary does not imply final verdict readiness while fields are missing");
            return expectations;
        }

        private List<String> problems(OfferTextParseResult result) {
            List<String> problems = new ArrayList<>();
            if (!result.isParsed()) {
                problems.add("result.isParsed() was false");
            }
            for (String expectedField : expectedExtractedFields) {
                if (!result.getExtractedFields().contains(expectedField)) {
                    problems.add("missing extracted signal '" + expectedField + "'");
                }
            }
            for (String missingField : expectedMissingFields) {
                if (!result.getMissingCriticalFields().contains(missingField)) {
                    problems.add("missing critical field was not requested: '" + missingField + "'");
                }
            }
            if (result.getMissingCriticalFields().isEmpty()) {
                problems.add("parser reported no missing critical fields for a low-information input");
            }
            if (expectConcernGate && (result.getSummary() == null
                    || !result.getSummary().contains("not enough for a final verdict"))) {
                problems.add("summary did not use the concern gate");
            }
            if (result.getSummary() != null
                    && result.getSummary().contains("Review the numbers, then run the report")) {
                problems.add("summary implied the report is ready");
            }
            return problems;
        }
    }

    private record City(String label, String expectedField) {
    }
}
