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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class OfferTextParserServiceAgent9ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports",
            "agent9-parser-money-scale-failures.txt");

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandAgent9AdversarialMoneyCases() throws IOException {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(signOnRetentionCases());
        cases.addAll(relocationMovingReimbursementCases());
        cases.addAll(insuranceParkingTuitionNoiseCases());
        cases.addAll(hourlyFormattingCases());
        cases.addAll(plusSlashSeparatorCrossWiringCases());

        assertEquals(1000, cases.size(), "agent 9 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) throws IOException {
        List<Failure> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.text(), "offer_review");
            List<String> mismatches = mismatches(spec, result);
            if (!mismatches.isEmpty()) {
                failures.add(new Failure(i, spec, result, mismatches));
            }
        });

        Files.createDirectories(FAILURE_REPORT.getParent());
        Files.writeString(FAILURE_REPORT, failureReport(cases.size(), failures));

        if (!failures.isEmpty()) {
            fail("Agent 9 money parser failures: " + failures.size() + " of " + cases.size()
                    + ". Categories: " + categorySummary(failures)
                    + ". Full report: " + FAILURE_REPORT.toAbsolutePath()
                    + "\n\n" + failureReportPreview(failures, 25));
        }
    }

    private List<CaseSpec> signOnRetentionCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            MoneySet money = money(i);
            Job job = job(i);
            String signOnText = switch (i % 5) {
                case 0 -> "sign-on bonus $" + comma(money.signOn());
                case 1 -> "sign on bonus " + k(money.signOn());
                case 2 -> "SOB $" + money.signOn();
                case 3 -> "retention bonus $" + comma(money.signOn());
                default -> "$" + comma(money.signOn()) + " commencement bonus";
            };
            String text = String.format(Locale.US,
                    "Current RN job in %s at $%.2f/hr. Offer is %s %s RN at $%.2f/hr, 36 hours. "
                            + "%s. Relocation stipend $%s. Moving cost estimate $%s. "
                            + "Current insurance $%s monthly; offer health insurance $%s monthly. "
                            + "Parking permit $%s and tuition reimbursement $%s are separate benefits.",
                    job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.unitLabel(),
                    job.offerRate(), signOnText, comma(money.relocation()), comma(money.movingCost()),
                    comma(money.currentInsurance()), comma(money.offerInsurance()), comma(money.parking()),
                    comma(money.tuition()));
            cases.add(new CaseSpec("sign-on-retention-" + i, text, job, money));
        }
        return cases;
    }

    private List<CaseSpec> relocationMovingReimbursementCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            MoneySet money = money(i + 200);
            Job job = job(i + 200);
            String relocationText = switch (i % 5) {
                case 0 -> "relocation assistance $" + comma(money.relocation());
                case 1 -> "relo " + k(money.relocation());
                case 2 -> "relocation stipend $" + money.relocation();
                case 3 -> "moving reimbursement $" + comma(money.relocation());
                default -> "$" + comma(money.relocation()) + " relocation";
            };
            String movingCostText = switch (i % 4) {
                case 0 -> "moving cost $" + comma(money.movingCost());
                case 1 -> "moving costs " + k(money.movingCost());
                case 2 -> "$" + comma(money.movingCost()) + " moving expense";
                default -> "move estimate $" + money.movingCost();
            };
            String text = String.format(Locale.US,
                    "Me now: %s RN, $%.2f per hour. New role: %s %s RN, $%.2f per hour, 3x12. "
                            + "Sign-on bonus $%s. %s. %s. Existing insurance $%s/mo; benefits premium $%s/mo. "
                            + "Do not count the $%s parking or $%s tuition line as relocation.",
                    job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.unitLabel(),
                    job.offerRate(), comma(money.signOn()), relocationText, movingCostText,
                    comma(money.currentInsurance()), comma(money.offerInsurance()), comma(money.parking()),
                    comma(money.tuition()));
            cases.add(new CaseSpec("relocation-moving-" + i, text, job, money));
        }
        return cases;
    }

    private List<CaseSpec> insuranceParkingTuitionNoiseCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            MoneySet money = money(i + 400);
            Job job = job(i + 400);
            String insuranceText = switch (i % 4) {
                case 0 -> "Current insurance $" + money.currentInsurance()
                        + " monthly; offer insurance premium $" + money.offerInsurance() + " monthly";
                case 1 -> "My insurance $" + money.currentInsurance()
                        + "/mo now, employee premium $" + money.offerInsurance() + "/mo on the offer";
                case 2 -> "Existing insurance $" + money.currentInsurance()
                        + " per month, health insurance $" + money.offerInsurance() + " per month at the new job";
                default -> "Current premium $" + money.currentInsurance()
                        + " monthly and medical premium $" + money.offerInsurance() + " monthly";
            };
            String text = String.format(Locale.US,
                    "Current: %s at $%.2f/hr. Offer: %s %s at $%.2f/hr for 36 hrs/wk. "
                            + "%s. Sign on bonus $%s; relocation assistance $%s; moving cost $%s. "
                            + "Noise amounts: parking $%s/month, tuition reimbursement $%s/year, scrub deposit $300.",
                    job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.unitLabel(),
                    job.offerRate(), insuranceText, comma(money.signOn()), comma(money.relocation()),
                    comma(money.movingCost()), comma(money.parking()), comma(money.tuition()));
            cases.add(new CaseSpec("insurance-noise-" + i, text, job, money));
        }
        return cases;
    }

    private List<CaseSpec> hourlyFormattingCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            MoneySet money = money(i + 600);
            Job job = job(i + 600);
            String rateText = switch (i % 5) {
                case 0 -> String.format(Locale.US, "current/%s/$%.2f/hr -> offer/%s/$%.2f/hr",
                        job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.offerRate());
                case 1 -> String.format(Locale.US, "I currently work in %s making $%.2f hourly; offer city %s base rate $%.2f hourly",
                        job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.offerRate());
                case 2 -> String.format(Locale.US, "old: %s at $%.2f per hour | new: %s at $%.2f per hour",
                        job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.offerRate());
                case 3 -> String.format(Locale.US, "now %s $%.2f/hour; new job located in %s $%.2f/hour",
                        job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.offerRate());
                default -> String.format(Locale.US, "Current job is in %s, rate $%.2f/hr. Offer in %s, rate $%.2f/hr",
                        job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.offerRate());
            };
            String text = String.format(Locale.US,
                    "%s. Unit %s RN, 36h nights. Sign-on bonus %s. Relocation stipend %s. "
                            + "Moving costs %s. Current insurance $%s; offer health insurance $%s. "
                            + "Parking $%s + tuition $%s should not become hourly rates.",
                    rateText, job.unitLabel(), k(money.signOn()), k(money.relocation()), k(money.movingCost()),
                    comma(money.currentInsurance()), comma(money.offerInsurance()), comma(money.parking()),
                    comma(money.tuition()));
            cases.add(new CaseSpec("hourly-formatting-" + i, text, job, money));
        }
        return cases;
    }

    private List<CaseSpec> plusSlashSeparatorCrossWiringCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            MoneySet money = money(i + 800);
            Job job = job(i + 800);
            String text = switch (i % 4) {
                case 0 -> String.format(Locale.US,
                        "Offer packet: %s/%s RN/$%.2f/hr + sign-on $%s + relocation $%s + moving cost $%s. "
                                + "Current/%s/$%.2f/hr. Current insurance $%s / offer insurance premium $%s. "
                                + "Parking $%s / tuition $%s.",
                        job.offerCity().label(), job.unitLabel(), job.offerRate(), comma(money.signOn()),
                        comma(money.relocation()), comma(money.movingCost()), job.currentCity().label(),
                        job.currentRate(), comma(money.currentInsurance()), comma(money.offerInsurance()),
                        comma(money.parking()), comma(money.tuition()));
                case 1 -> String.format(Locale.US,
                        "Current %s $%.2f/hr / offer %s %s $%.2f/hr / SOB $%s / relo $%s / move estimate $%s / "
                                + "my insurance $%s / employee premium $%s / parking $%s / tuition $%s",
                        job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.unitLabel(),
                        job.offerRate(), comma(money.signOn()), comma(money.relocation()), comma(money.movingCost()),
                        comma(money.currentInsurance()), comma(money.offerInsurance()), comma(money.parking()),
                        comma(money.tuition()));
                case 2 -> String.format(Locale.US,
                        "%s $%.2f/hr now + %s $%.2f/hr offer + %s RN + retention bonus $%s + moving reimbursement $%s + "
                                + "moving expense $%s + current premium $%s + benefits premium $%s + parking $%s + tuition $%s",
                        job.currentCity().label(), job.currentRate(), job.offerCity().label(), job.offerRate(),
                        job.unitLabel(), comma(money.signOn()), comma(money.relocation()), comma(money.movingCost()),
                        comma(money.currentInsurance()), comma(money.offerInsurance()), comma(money.parking()),
                        comma(money.tuition()));
                default -> String.format(Locale.US,
                        "New role located in %s: %s RN at $%.2f/hr; sign on %s; relocation assistance %s; moving costs %s. "
                                + "Current role currently in %s at $%.2f/hr; current insurance $%s; medical premium $%s. "
                                + "Parking is $%s, tuition reimbursement is $%s.",
                        job.offerCity().label(), job.unitLabel(), job.offerRate(), k(money.signOn()),
                        k(money.relocation()), k(money.movingCost()), job.currentCity().label(), job.currentRate(),
                        comma(money.currentInsurance()), comma(money.offerInsurance()), comma(money.parking()),
                        comma(money.tuition()));
            };
            cases.add(new CaseSpec("plus-slash-cross-wire-" + i, text, job, money));
        }
        return cases;
    }

    private List<String> mismatches(CaseSpec spec, OfferTextParseResult result) {
        OfferRiskDraft draft = result.getDraft();
        List<String> mismatches = new ArrayList<>();
        requireClose(mismatches, "currentHourlyRate", spec.job().currentRate(), draft.getCurrentHourlyRate());
        requireClose(mismatches, "offerHourlyRate", spec.job().offerRate(), draft.getOfferHourlyRate());
        requireClose(mismatches, "weeklyHours", 36.0, draft.getWeeklyHours());
        requireEquals(mismatches, "unitType", spec.job().unitSlug(), draft.getUnitType());
        requireClose(mismatches, "signOnBonus", spec.money().signOn(), draft.getSignOnBonus());
        requireClose(mismatches, "relocationStipend", spec.money().relocation(), draft.getRelocationStipend());
        requireClose(mismatches, "movingCost", spec.money().movingCost(), draft.getMovingCost());
        requireClose(mismatches, "currentMonthlyInsurance", spec.money().currentInsurance(),
                draft.getCurrentMonthlyInsurance());
        requireClose(mismatches, "offerMonthlyInsurance", spec.money().offerInsurance(),
                draft.getOfferMonthlyInsurance());
        requireNotCrossWired(mismatches, "signOnBonus", spec.money().signOn(), draft.getSignOnBonus(), spec.money());
        requireNotCrossWired(mismatches, "relocationStipend", spec.money().relocation(),
                draft.getRelocationStipend(), spec.money());
        requireNotCrossWired(mismatches, "movingCost", spec.money().movingCost(), draft.getMovingCost(), spec.money());
        requireNotCrossWired(mismatches, "offerMonthlyInsurance", spec.money().offerInsurance(),
                draft.getOfferMonthlyInsurance(), spec.money());
        assertTrue(result.isParsed(), "case should parse at least one field");
        return mismatches;
    }

    private void requireEquals(List<String> mismatches, String field, String expected, String actual) {
        if (!expected.equals(actual)) {
            mismatches.add(field + " expected=" + expected + " actual=" + actual);
        }
    }

    private void requireClose(List<String> mismatches, String field, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.01) {
            mismatches.add(field + " expected=" + expected + " actual=" + actual);
        }
    }

    private void requireNotCrossWired(List<String> mismatches, String field, double expected, double actual,
                                      MoneySet money) {
        if (Math.abs(actual - expected) <= 0.01) {
            return;
        }
        if (Math.abs(actual - money.parking()) <= 0.01) {
            mismatches.add(field + " cross-wired parking amount=" + actual);
        }
        if (Math.abs(actual - money.tuition()) <= 0.01) {
            mismatches.add(field + " cross-wired tuition amount=" + actual);
        }
    }

    private String failureReport(int totalCases, List<Failure> failures) {
        if (failures.isEmpty()) {
            return "PASS: 1000 Agent 9 adversarial money parser cases passed.\n";
        }
        return "FAIL: " + failures.size() + " of " + totalCases + " cases failed.\n"
                + "Categories: " + categorySummary(failures) + "\n\n"
                + failureReportPreview(failures, failures.size());
    }

    private String failureReportPreview(List<Failure> failures, int limit) {
        return String.join("\n\n", failures.stream()
                .limit(limit)
                .map(Failure::format)
                .toList());
    }

    private String categorySummary(List<Failure> failures) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Failure failure : failures) {
            for (String mismatch : failure.mismatches()) {
                String category = mismatch.split(" ", 2)[0];
                counts.put(category, counts.getOrDefault(category, 0) + 1);
            }
        }
        return counts.toString();
    }

    private Job job(int i) {
        City[] cities = {
                new City("Austin, TX", "austin-tx"),
                new City("Seattle, WA", "seattle-wa"),
                new City("Phoenix, AZ", "phoenix-az"),
                new City("Los Angeles, CA", "los-angeles-ca"),
                new City("New York City", "new-york-ny")
        };
        Unit[] units = {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("med surg tele", "med_surg")
        };
        City current = cities[i % cities.length];
        City offer = cities[(i + 2) % cities.length];
        Unit unit = units[i % units.length];
        double currentRate = 41.25 + (i % 9) + ((i % 4) * 0.10);
        double offerRate = 56.50 + (i % 11) + ((i % 5) * 0.10);
        return new Job(current, offer, currentRate, offerRate, unit.label(), unit.slug());
    }

    private MoneySet money(int i) {
        int signOn = 9000 + (i % 9) * 1000;
        int relocation = 3000 + (i % 7) * 500;
        int movingCost = 6500 + (i % 8) * 250;
        int currentInsurance = 180 + (i % 6) * 15;
        int offerInsurance = 260 + (i % 7) * 20;
        int parking = 70 + (i % 10) * 10;
        int tuition = 2500 + (i % 9) * 750;
        return new MoneySet(signOn, relocation, movingCost, currentInsurance, offerInsurance, parking, tuition);
    }

    private String comma(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private String k(int value) {
        return (value % 1000 == 0)
                ? (value / 1000) + "k"
                : String.format(Locale.US, "%.2fk", value / 1000.0);
    }

    private record City(String label, String slug) {
    }

    private record Unit(String label, String slug) {
    }

    private record Job(City currentCity,
                       City offerCity,
                       double currentRate,
                       double offerRate,
                       String unitLabel,
                       String unitSlug) {
    }

    private record MoneySet(int signOn,
                            int relocation,
                            int movingCost,
                            int currentInsurance,
                            int offerInsurance,
                            int parking,
                            int tuition) {
    }

    private record CaseSpec(String name, String text, Job job, MoneySet money) {
    }

    private record Failure(int index,
                           CaseSpec spec,
                           OfferTextParseResult result,
                           List<String> mismatches) {

        private String format() {
            OfferRiskDraft draft = result.getDraft();
            return """
                    CASE %d [%s]
                    INPUT: %s
                    EXPECTED: currentCity=%s, offerCity=%s, currentRate=%.2f, offerRate=%.2f, signOn=%.2f, relocation=%.2f, movingCost=%.2f, currentInsurance=%.2f, offerInsurance=%.2f
                    ACTUAL: currentCity=%s, offerCity=%s, currentRate=%.2f, offerRate=%.2f, signOn=%.2f, relocation=%.2f, movingCost=%.2f, currentInsurance=%.2f, offerInsurance=%.2f
                    MISMATCHES: %s
                    EXTRACTED: %s
                    MISSING: %s
                    WARNING: %s
                    """.formatted(index, spec.name(), spec.text(), spec.job().currentCity().slug(),
                    spec.job().offerCity().slug(), spec.job().currentRate(), spec.job().offerRate(),
                    (double) spec.money().signOn(), (double) spec.money().relocation(),
                    (double) spec.money().movingCost(), (double) spec.money().currentInsurance(),
                    (double) spec.money().offerInsurance(), draft.getCurrentCitySlug(), draft.getOfferCitySlug(),
                    draft.getCurrentHourlyRate(), draft.getOfferHourlyRate(), draft.getSignOnBonus(),
                    draft.getRelocationStipend(), draft.getMovingCost(), draft.getCurrentMonthlyInsurance(),
                    draft.getOfferMonthlyInsurance(), mismatches, result.getExtractedFields(),
                    result.getMissingCriticalFields(), result.getParseWarning());
        }
    }
}
