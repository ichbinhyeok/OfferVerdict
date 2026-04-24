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

class OfferTextParserServiceAgent7ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports", "agent7-parser-failures.txt");

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandAgent7AdversarialJobPostAndMoneyCases() {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(jobPostRangeCases());
        cases.addAll(ocrNoisyCopiedListingCases());
        cases.addAll(marketingCityNoiseCases());
        cases.addAll(primaryLocationLineCases());
        cases.addAll(fteCases());
        cases.addAll(shiftAndWeekendDifferentialCases());
        cases.addAll(signOnRelocationSeparationCases());
        cases.addAll(offerReviewCurrentVsOfferCases());

        assertEquals(1000, cases.size(), "Agent 7 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.input(), spec.analysisMode());
            OfferRiskDraft draft = result.getDraft();
            List<String> mismatches = new ArrayList<>();

            if (!result.isParsed()) {
                mismatches.add("parsed expected <true> but was <false>");
            }
            expectEquals(mismatches, "offerCitySlug", spec.offerCitySlug(), draft.getOfferCitySlug());
            expectEquals(mismatches, "currentCitySlug", spec.currentCitySlug(), draft.getCurrentCitySlug());
            expectDouble(mismatches, "offerHourlyRate", spec.offerHourlyRate(), draft.getOfferHourlyRate());
            expectDouble(mismatches, "currentHourlyRate", spec.currentHourlyRate(), draft.getCurrentHourlyRate());
            expectDouble(mismatches, "weeklyHours", spec.weeklyHours(), draft.getWeeklyHours());
            expectDouble(mismatches, "nightDiffPercent", spec.nightDiffPercent(), draft.getNightDiffPercent());
            expectDouble(mismatches, "weekendDiffPercent", spec.weekendDiffPercent(), draft.getWeekendDiffPercent());
            expectDouble(mismatches, "signOnBonus", spec.signOnBonus(), draft.getSignOnBonus());
            expectDouble(mismatches, "relocationStipend", spec.relocationStipend(), draft.getRelocationStipend());
            expectEquals(mismatches, "unitType", spec.unitType(), draft.getUnitType());

            if (!mismatches.isEmpty()) {
                failures.add(formatFailure(i, spec, draft, result, mismatches));
            }
        });

        if (!failures.isEmpty()) {
            writeFailureReport(failures);
            fail("Agent 7 adversarial parser failures: " + failures.size()
                    + " of " + cases.size()
                    + ". Unique failure categories: " + categoryCounts(failures)
                    + ". Full report: " + FAILURE_REPORT.toAbsolutePath()
                    + "\n\n" + String.join("\n\n", failures.stream().limit(40).toList()));
        }
    }

    private List<CaseSpec> jobPostRangeCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City city = offerCities()[i % offerCities().length];
            Unit unit = units()[i % units().length];
            double low = 48 + (i % 9);
            double high = low + 28 + (i % 6);
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "RN job posting: %s %s. Posted pay range is $%.2f to $%.2f per hour, 36 hours per week, nights.",
                        city.label(), unit.label(), low, high);
                case 1 -> String.format(Locale.US,
                        "Open role - %s registered nurse in %s. Salary range $%.2f-$%.2f/hourly. Three 12s.",
                        unit.label(), city.label(), low, high);
                case 2 -> String.format(Locale.US,
                        "Position located in %s: %s RN. Pay Range Minimum $%.2f hourly Pay Range Maximum $%.2f hourly.",
                        city.label(), unit.label(), low, high);
                case 3 -> String.format(Locale.US,
                        "Job post for %s, %s. Base compensation $%.2f to $%.2f/hr; 0.90 FTE.",
                        city.label(), unit.label(), low, high);
                default -> String.format(Locale.US,
                        "%s hiring notice. Unit %s. Posted pay $%.2f to $%.2f per hour plus differentials.",
                        city.label(), unit.label(), low, high);
            };
            Double expectedHours = i % 5 == 3 ? Double.valueOf(36.0) : null;
            cases.add(CaseSpec.jobPost("job-post-range", input, city.slug(), low, expectedHours, null, null,
                    null, null, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> ocrNoisyCopiedListingCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City city = offerCities()[(i + 1) % offerCities().length];
            Unit unit = units()[(i + 2) % units().length];
            double low = 46 + (i % 8);
            double high = low + 31;
            double signOn = 8000 + (i % 5) * 1000;
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Copied listing >>> JOB POST / %s / %s RN / Pay Range Minimum: $%.2f Pay Range Maximum: $%.2f / Sign-On Bonus $%.0f / NOC.",
                        city.label(), unit.label(), low, high, signOn);
                case 1 -> String.format(Locale.US,
                        "OCR text: Position located in %s || dept=%s || pay range $%.2f to $%.2f hourly || SOB $%.0f.",
                        city.label(), unit.label(), low, high, signOn);
                case 2 -> String.format(Locale.US,
                        "Pasted from PDF: %s registered nurse, %s; raNge minimum $%.2f; range maximum $%.2f; sign on bonus $%.0f.",
                        city.label(), unit.label(), low, high, signOn);
                case 3 -> String.format(Locale.US,
                        "JOB LISTING %s -- %s -- posted pay $%.2f to $%.2f per hour -- bonus $%.0f -- 36 hrs/wk.",
                        city.label(), unit.label(), low, high, signOn);
                default -> String.format(Locale.US,
                        "Noisy copy: %s   %s RN   Base rate $%.2f to $%.2f/hourly   commencement bonus $%.0f.",
                        city.label(), unit.label(), low, high, signOn);
            };
            cases.add(CaseSpec.jobPost("ocr-noisy-copied-listing", input, city.slug(), low,
                    i % 5 == 3 ? 36.0 : null, null, null, signOn, null, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> marketingCityNoiseCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] noiseCities = {
                new City("Boston, MA", "boston-ma"),
                new City("Chicago, IL", "chicago-il"),
                new City("Houston, TX", "houston-tx"),
                new City("Philadelphia, PA", "philadelphia-pa")
        };
        for (int i = 0; i < 125; i++) {
            City city = offerCities()[(i + 2) % offerCities().length];
            City noise = noiseCities[i % noiseCities.length];
            Unit unit = units()[(i + 1) % units().length];
            double low = 50 + (i % 7);
            double high = low + 34;
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Marketing says our network spans %s and other cities. Actual job location: %s. %s RN. Pay range $%.2f to $%.2f per hour.",
                        noise.label(), city.label(), unit.label(), low, high);
                case 1 -> String.format(Locale.US,
                        "Do not use the career fair city %s. Primary work location %s for %s registered nurse, $%.2f-$%.2f/hourly.",
                        noise.label(), city.label(), unit.label(), low, high);
                case 2 -> String.format(Locale.US,
                        "Hospital blog mentions %s cost of living. Job posting location is %s; %s; pay range $%.2f to $%.2f per hour.",
                        noise.label(), city.label(), unit.label(), low, high);
                case 3 -> String.format(Locale.US,
                        "Recruiting roadshow: %s. Open role located in %s, %s RN, base rate $%.2f/hr.",
                        noise.label(), city.label(), unit.label(), low);
                default -> String.format(Locale.US,
                        "Banner: hiring from %s to everywhere. Offer city %s, %s role, posted pay $%.2f to $%.2f hourly.",
                        noise.label(), city.label(), unit.label(), low, high);
            };
            cases.add(CaseSpec.jobPost("marketing-city-noise", input, city.slug(), low, null, null, null,
                    null, null, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> primaryLocationLineCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City city = offerCities()[(i + 3) % offerCities().length];
            Unit unit = units()[(i + 3) % units().length];
            double rate = 54 + (i % 10);
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Title: RN - %s\nPrimary work location: %s\nBase rate: $%.2f/hr\nSchedule: 36 hrs/wk",
                        unit.label(), city.label(), rate);
                case 1 -> String.format(Locale.US,
                        "Department %s\nJob location: %s\nOffer base rate $%.2f per hour\nShift: nights",
                        unit.label(), city.label(), rate);
                case 2 -> String.format(Locale.US,
                        "Posting header lists corporate office elsewhere.\nPrimary Location - %s\n%s RN\n$%.2f/hr.",
                        city.label(), unit.label(), rate);
                case 3 -> String.format(Locale.US,
                        "Open role\nLocation: %s\nUnit: %s\nCompensation: $%.2f/hourly\nFTE: 1.0 FTE",
                        city.label(), unit.label(), rate);
                default -> String.format(Locale.US,
                        "New job location %s; %s registered nurse; base rate $%.2f/hr; three 12s.",
                        city.label(), unit.label(), rate);
            };
            Double expectedHours = switch (i % 5) {
                case 0, 4 -> 36.0;
                case 3 -> 40.0;
                default -> null;
            };
            cases.add(CaseSpec.jobPost("primary-location-line", input, city.slug(), rate, expectedHours,
                    null, null, null, null, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> fteCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City city = offerCities()[(i + 4) % offerCities().length];
            Unit unit = units()[(i + 4) % units().length];
            double rate = 52 + (i % 8);
            String input;
            double hours;
            switch (i % 5) {
                case 0 -> {
                    input = String.format(Locale.US,
                            "%s %s RN opening. Base rate $%.2f/hr. Schedule 80%% FTE nights.",
                            city.label(), unit.label(), rate);
                    hours = 32.0;
                }
                case 1 -> {
                    input = String.format(Locale.US,
                            "Job posting: %s, %s, $%.2f per hour, 90%% FTE.",
                            city.label(), unit.label(), rate);
                    hours = 36.0;
                }
                case 2 -> {
                    input = String.format(Locale.US,
                            "Position located in %s for %s RN. $%.2f/hourly. 0.75 FTE.",
                            city.label(), unit.label(), rate);
                    hours = 30.0;
                }
                case 3 -> {
                    input = String.format(Locale.US,
                            "%s %s registered nurse. Offer base rate $%.2f/hr. 0.8 FTE.",
                            city.label(), unit.label(), rate);
                    hours = 32.0;
                }
                default -> {
                    input = String.format(Locale.US,
                            "New job in %s, %s unit, base rate $%.2f/hr, 1.0 FTE.",
                            city.label(), unit.label(), rate);
                    hours = 40.0;
                }
            }
            cases.add(CaseSpec.jobPost("fte-hours", input, city.slug(), rate, hours,
                    null, null, null, null, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> shiftAndWeekendDifferentialCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City city = offerCities()[(i + 5) % offerCities().length];
            Unit unit = units()[(i + 5) % units().length];
            double rate = 55 + (i % 6);
            String input;
            double night;
            double weekend;
            switch (i % 5) {
                case 0 -> {
                    night = 12;
                    weekend = 8;
                    input = String.format(Locale.US,
                            "%s %s RN job posting. Base rate $%.2f/hr. Night shift differential 12%%. Weekend differential 8%%.",
                            city.label(), unit.label(), rate);
                }
                case 1 -> {
                    night = dollarsToPercent(5, rate);
                    weekend = dollarsToPercent(4, rate);
                    input = String.format(Locale.US,
                            "Position located in %s, %s. Base rate $%.2f/hr. Night differential $5/hr and weekend premium $4/hr.",
                            city.label(), unit.label(), rate);
                }
                case 2 -> {
                    night = 10;
                    weekend = dollarsToPercent(3, rate);
                    input = String.format(Locale.US,
                            "%s %s registered nurse offer base rate $%.2f/hr, night premium 10%%, weekend differential $3.",
                            city.label(), unit.label(), rate);
                }
                case 3 -> {
                    night = dollarsToPercent(6, rate);
                    weekend = 7;
                    input = String.format(Locale.US,
                            "Job location %s. %s RN. $%.2f hourly. Night shift premium $6/hr. Weekend shift premium 7%%.",
                            city.label(), unit.label(), rate);
                }
                default -> {
                    night = 15;
                    weekend = 9;
                    input = String.format(Locale.US,
                            "Open role in %s: %s RN, base rate $%.2f/hr, night diff 15%%, weekend diff 9%%.",
                            city.label(), unit.label(), rate);
                }
            }
            cases.add(CaseSpec.jobPost("shift-weekend-differentials", input, city.slug(), rate, null,
                    night, weekend, null, null, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> signOnRelocationSeparationCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City city = offerCities()[(i + 6) % offerCities().length];
            Unit unit = units()[(i + 6) % units().length];
            double rate = 56 + (i % 7);
            double signOn = 9000 + (i % 6) * 1000;
            double relocation = 3000 + (i % 5) * 1000;
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "%s %s RN offer base rate $%.2f/hr. Sign-on bonus $%.0f. Relocation stipend $%.0f.",
                        city.label(), unit.label(), rate, signOn, relocation);
                case 1 -> String.format(Locale.US,
                        "Position located in %s, %s. $%.2f/hr. Relocation assistance $%.0f; sign on bonus $%.0f.",
                        city.label(), unit.label(), rate, relocation, signOn);
                case 2 -> String.format(Locale.US,
                        "%s %s job post: hourly $%.2f/hr, SOB $%.0f, relo $%.0f.",
                        city.label(), unit.label(), rate, signOn, relocation);
                case 3 -> String.format(Locale.US,
                        "New role %s %s RN. Base rate $%.2f per hour. $%.0f commencement bonus and $%.0f moving reimbursement.",
                        city.label(), unit.label(), rate, signOn, relocation);
                default -> String.format(Locale.US,
                        "Offer city %s; %s; $%.2f/hourly; retention bonus $%.0f; relocation support $%.0f.",
                        city.label(), unit.label(), rate, signOn, relocation);
            };
            cases.add(CaseSpec.jobPost("signon-relocation-separation", input, city.slug(), rate,
                    null, null, null, signOn, relocation, unit.slug()));
        }
        return cases;
    }

    private List<CaseSpec> offerReviewCurrentVsOfferCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 125; i++) {
            City current = currentCities()[i % currentCities().length];
            City offer = offerCities()[(i + 1) % offerCities().length];
            Unit unit = units()[i % units().length];
            double currentRate = 41 + (i % 7);
            double offerRate = 58 + (i % 8);
            double signOn = 10000 + (i % 4) * 1000;
            double relocation = 4000 + (i % 3) * 1000;
            String input = switch (i % 5) {
                case 0 -> String.format(Locale.US,
                        "Current job: %s $%.2f/hr. New job location %s, %s RN, offer base rate $%.2f/hr, sign-on $%.0f, relocation $%.0f.",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate, signOn, relocation);
                case 1 -> String.format(Locale.US,
                        "I currently work in %s making $%.2f/hr. Position located in %s for %s, $%.2f/hr, relo $%.0f, SOB $%.0f.",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate, relocation, signOn);
                case 2 -> String.format(Locale.US,
                        "RN decision: %s $%.2f/hr now -> %s $%.2f/hr offer. Unit %s. Sign on bonus $%.0f. Relocation assistance $%.0f.",
                        current.label(), currentRate, offer.label(), offerRate, unit.label(), signOn, relocation);
                case 3 -> String.format(Locale.US,
                        "Existing job in %s at $%.2f per hour; offer city %s; %s role; base rate $%.2f per hour; moving reimbursement $%.0f; bonus $%.0f.",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate, relocation, signOn);
                default -> String.format(Locale.US,
                        "Me now - %s - $%.2f/hr. New RN job - %s - %s - $%.2f/hr - $%.0f retention bonus - $%.0f relocation stipend.",
                        current.label(), currentRate, offer.label(), unit.label(), offerRate, signOn, relocation);
            };
            cases.add(new CaseSpec("offer-review-current-vs-offer", "offer_review", input, offer.slug(),
                    current.slug(), offerRate, currentRate, null, null, null, signOn, relocation, unit.slug()));
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

    private static double dollarsToPercent(double dollars, double baseRate) {
        return (dollars / baseRate) * 100.0;
    }

    private static void expectEquals(List<String> mismatches, String field, String expected, String actual) {
        if (expected != null && !expected.equals(actual)) {
            mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void expectDouble(List<String> mismatches, String field, Double expected, double actual) {
        if (expected != null && Math.abs(expected - actual) > 0.05) {
            mismatches.add(field + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static String formatFailure(int index,
                                        CaseSpec spec,
                                        OfferRiskDraft draft,
                                        OfferTextParseResult result,
                                        List<String> mismatches) {
        return "category=" + spec.category()
                + " case=" + index
                + "\nINPUT: " + spec.input()
                + "\nEXPECTED: offerCitySlug=" + spec.offerCitySlug()
                + ", currentCitySlug=" + spec.currentCitySlug()
                + ", offerHourlyRate=" + spec.offerHourlyRate()
                + ", currentHourlyRate=" + spec.currentHourlyRate()
                + ", weeklyHours=" + spec.weeklyHours()
                + ", nightDiffPercent=" + spec.nightDiffPercent()
                + ", weekendDiffPercent=" + spec.weekendDiffPercent()
                + ", signOnBonus=" + spec.signOnBonus()
                + ", relocationStipend=" + spec.relocationStipend()
                + ", unitType=" + spec.unitType()
                + "\nACTUAL: offerCitySlug=" + draft.getOfferCitySlug()
                + ", currentCitySlug=" + draft.getCurrentCitySlug()
                + ", offerHourlyRate=" + draft.getOfferHourlyRate()
                + ", currentHourlyRate=" + draft.getCurrentHourlyRate()
                + ", weeklyHours=" + draft.getWeeklyHours()
                + ", nightDiffPercent=" + draft.getNightDiffPercent()
                + ", weekendDiffPercent=" + draft.getWeekendDiffPercent()
                + ", signOnBonus=" + draft.getSignOnBonus()
                + ", relocationStipend=" + draft.getRelocationStipend()
                + ", unitType=" + draft.getUnitType()
                + "\nMISMATCHES: " + mismatches
                + "\nEXTRACTED: " + result.getExtractedFields()
                + "\nMISSING: " + result.getMissingCriticalFields()
                + "\nWARNING: " + result.getParseWarning();
    }

    private static Map<String, Integer> categoryCounts(List<String> failures) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String failure : failures) {
            int start = "category=".length();
            int end = failure.indexOf(' ', start);
            String category = end > start ? failure.substring(start, end) : "unknown";
            counts.merge(category, 1, Integer::sum);
        }
        return counts;
    }

    private static void writeFailureReport(List<String> failures) {
        try {
            Files.createDirectories(FAILURE_REPORT.getParent());
            Files.writeString(FAILURE_REPORT, String.join("\n\n", failures));
        } catch (IOException ignored) {
            // The assertion message still includes representative failures.
        }
    }

    private record City(String label, String slug) {
    }

    private record Unit(String label, String slug) {
    }

    private record CaseSpec(String category,
                            String analysisMode,
                            String input,
                            String offerCitySlug,
                            String currentCitySlug,
                            Double offerHourlyRate,
                            Double currentHourlyRate,
                            Double weeklyHours,
                            Double nightDiffPercent,
                            Double weekendDiffPercent,
                            Double signOnBonus,
                            Double relocationStipend,
                            String unitType) {

        private static CaseSpec jobPost(String category,
                                        String input,
                                        String offerCitySlug,
                                        Double offerHourlyRate,
                                        Double weeklyHours,
                                        Double nightDiffPercent,
                                        Double weekendDiffPercent,
                                        Double signOnBonus,
                                        Double relocationStipend,
                                        String unitType) {
            return new CaseSpec(category, "job_post", input, offerCitySlug, null, offerHourlyRate, null,
                    weeklyHours, nightDiffPercent, weekendDiffPercent, signOnBonus, relocationStipend, unitType);
        }
    }
}
