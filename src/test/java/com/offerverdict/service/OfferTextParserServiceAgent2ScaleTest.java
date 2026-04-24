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

class OfferTextParserServiceAgent2ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports",
            "agent2-parser-scale-failures.txt");

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandAgent2AdversarialJobPostInputs() throws IOException {
        List<CaseSpec> cases = new ArrayList<>();
        cases.addAll(multiCityJobPostCases());
        cases.addAll(ocrLikeNoiseCases());
        cases.addAll(payRangeAndMalformedRangeCases());
        cases.addAll(fteAndPremiumCases());
        cases.addAll(brochureAndRelocationNoiseCases());

        assertEquals(1000, cases.size(), "agent 2 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) throws IOException {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.text(), "job_post");
            try {
                spec.assertResult(result);
            } catch (AssertionError error) {
                OfferRiskDraft draft = result.getDraft();
                failures.add("""
                        CASE %d [%s]
                        INPUT: %s
                        EXPECTED: %s
                        ACTUAL: city=%s, rate=%.2f, hours=%.2f, night=%.4f, weekend=%.4f, signOn=%.2f, relocation=%.2f, missing=%s, extracted=%s, warning=%s
                        ERROR: %s
                        """.formatted(i, spec.name(), spec.text(), spec.expected(),
                        draft.getOfferCitySlug(), draft.getOfferHourlyRate(), draft.getWeeklyHours(),
                        draft.getNightDiffPercent(), draft.getWeekendDiffPercent(), draft.getSignOnBonus(),
                        draft.getRelocationStipend(), result.getMissingCriticalFields(),
                        result.getExtractedFields(), result.getParseWarning(), error.getMessage()));
            }
        });

        Files.createDirectories(FAILURE_REPORT.getParent());
        Files.writeString(FAILURE_REPORT, failures.isEmpty()
                ? "PASS: 1000 Agent 2 adversarial parser cases passed.\n"
                : String.join("\n", failures));
        assertTrue(failures.isEmpty(), "Agent 2 parser failures written to " + FAILURE_REPORT.toAbsolutePath()
                + "\n\n" + String.join("\n", failures.stream().limit(25).toList()));
    }

    private List<CaseSpec> multiCityJobPostCases() {
        List<CaseSpec> cases = new ArrayList<>();
        Job[] jobs = {
                new Job("Seattle, WA", "seattle-wa", "ICU", "$56.25 - $91.20/hr", 56.25, 36,
                        10000, 4000),
                new Job("Los Angeles, CA", "los-angeles-ca", "ED", "$58.10 to $87.40 hourly", 58.10, 36,
                        7500, 5000),
                new Job("New York City", "new-york-ny", "med surg telemetry", "$62.00-$96.00 per hour", 62.00,
                        36, 12000, 6000),
                new Job("Phoenix, AZ", "phoenix-az", "ED", "$45.50 - $72.20/hr", 45.50, 36,
                        6000, 3000)
        };
        String[] nearbyBlurbs = {
                "Nearby destinations: Portland 174 miles, Tacoma 33 miles, Austin 2135 miles.",
                "Regional page mentions Seattle, Phoenix, and Austin for comparison, but this opening is local.",
                "Travel guide: beaches, downtown, New York flights, Los Angeles connections.",
                "Cost-of-living comparison: Austin median rent $1850, Phoenix $1620, Seattle $2400."
        };

        for (int i = 0; i < 200; i++) {
            Job job = jobs[i % jobs.length];
            String text = String.format(Locale.US,
                    "%s RN job post - %s unit. %s Posted pay range %s. Schedule 36 hours per week, nights. Sign-on bonus $%d. Relocation stipend $%d. %s",
                    job.cityLabel(), job.unit(), nearbyBlurbs[i % nearbyBlurbs.length], job.rangeText(),
                    job.signOn(), job.relocation(), nearbyBlurbs[(i + 1) % nearbyBlurbs.length]);
            cases.add(new CaseSpec("multi-city-job-post-" + i, text,
                    "offer city " + job.citySlug() + ", low end rate " + job.expectedRate()
                            + ", sign-on " + job.signOn() + ", relocation " + job.relocation(),
                    result -> {
                        OfferRiskDraft draft = result.getDraft();
                        assertTrue(result.isParsed());
                        assertEquals(job.citySlug(), draft.getOfferCitySlug());
                        assertEquals(job.expectedRate(), draft.getOfferHourlyRate(), 0.01);
                        assertEquals(job.hours(), draft.getWeeklyHours(), 0.01);
                        assertEquals(job.signOn(), draft.getSignOnBonus(), 0.01);
                        assertEquals(job.relocation(), draft.getRelocationStipend(), 0.01);
                    }));
        }
        return cases;
    }

    private List<CaseSpec> ocrLikeNoiseCases() {
        List<CaseSpec> cases = new ArrayList<>();
        OcrJob[] jobs = {
                new OcrJob("Seattle WA", "seattle-wa", "ICU", 57.80, 90, 10000, 4500),
                new OcrJob("Los Angeles CA", "los-angeles-ca", "ED", 59.25, 80, 8000, 5000),
                new OcrJob("New York City", "new-york-ny", "med surg tele", 63.10, 100, 13000, 6000),
                new OcrJob("Phoenix AZ", "phoenix-az", "ED", 46.40, 90, 6500, 3500)
        };
        String[] headers = {
                "BENEF1TS OVERV1EW | equal opportunity employer | page 2 of 7",
                "H0SPITAL CAREER BR0CHURE scan text | campus map | cafeteria hours",
                "RN RECRU1TING PDF OCR | rn rn rn | parking permit $95 monthly",
                "SYSTEM GENERATED POSTING | nearby clinic 14 mi | population 742000"
        };

        for (int i = 0; i < 200; i++) {
            OcrJob job = jobs[i % jobs.length];
            double expectedHours = job.ftePercent() / 100.0 * 40.0;
            String text = String.format(Locale.US,
                    "%s\nJOB L0CATI0N: %s\nR0LE: Registered Nurse - %s\nPAY: base rate $%.2f/hr\nFTE: %d%% FTE nights\nN1GHT premium $6/hr; WEEKEND premium $4/hr\nS1GN-ON bonus $%d\nREL0CATION stipend $%d\nBrochure says Austin and Seattle are great cities; ignore as marketing copy.",
                    headers[i % headers.length], job.cityLabel(), job.unit(), job.rate(), job.ftePercent(),
                    job.signOn(), job.relocation());
            cases.add(new CaseSpec("ocr-noise-" + i, text,
                    "offer city " + job.citySlug() + ", base rate " + job.rate()
                            + ", FTE hours " + expectedHours + ", sign-on " + job.signOn(),
                    result -> {
                        OfferRiskDraft draft = result.getDraft();
                        assertTrue(result.isParsed());
                        assertEquals(job.citySlug(), draft.getOfferCitySlug());
                        assertEquals(job.rate(), draft.getOfferHourlyRate(), 0.01);
                        assertEquals(expectedHours, draft.getWeeklyHours(), 0.01);
                        assertEquals(job.signOn(), draft.getSignOnBonus(), 0.01);
                        assertEquals(job.relocation(), draft.getRelocationStipend(), 0.01);
                    }));
        }
        return cases;
    }

    private List<CaseSpec> payRangeAndMalformedRangeCases() {
        List<CaseSpec> cases = new ArrayList<>();
        RangeJob[] jobs = {
                new RangeJob("Seattle, WA", "seattle-wa", "Pay Range Minimum: $54.25 hourly Pay Range Maximum: $91.22 hourly", 54.25, 9000),
                new RangeJob("Los Angeles, CA", "los-angeles-ca", "Salary Range $57.75 - $88.40/hr", 57.75, 8500),
                new RangeJob("New York City", "new-york-ny", "Compensation: $61.30 to $97.10 per hour", 61.30, 12500),
                new RangeJob("Phoenix, AZ", "phoenix-az", "Pay range min $44.80 hourly max $73.60 hourly", 44.80, 6500)
        };
        String[] malformedNoise = {
                "The brochure also says new graduate workshop 8-12 weeks and population 1,200,000.",
                "Range may display as $0 during HR sync; use posted minimum and maximum above.",
                "Nearby city rate examples: Austin $42/hr, Seattle $60/hr, Los Angeles $58/hr.",
                "Weekend diff may vary by contract, estimated at $4/hr, not the base range."
        };

        for (int i = 0; i < 200; i++) {
            RangeJob job = jobs[i % jobs.length];
            int relocation = 3000 + (i % 5) * 500;
            String text = String.format(Locale.US,
                    "Registered Nurse job posting. Location: %s. Unit: ICU/ED float pool. %s. 0.9 FTE, night shift. Sign-on bonus: $%d. Relocation assistance: $%d. %s",
                    job.cityLabel(), job.rangeText(), job.signOn(), relocation, malformedNoise[i % malformedNoise.length]);
            cases.add(new CaseSpec("pay-range-" + i, text,
                    "offer city " + job.citySlug() + ", selected low range " + job.lowRate()
                            + ", 0.9 FTE hours 36.0, sign-on " + job.signOn(),
                    result -> {
                        OfferRiskDraft draft = result.getDraft();
                        assertTrue(result.isParsed());
                        assertEquals(job.citySlug(), draft.getOfferCitySlug());
                        assertEquals(job.lowRate(), draft.getOfferHourlyRate(), 0.01);
                        assertEquals(36.0, draft.getWeeklyHours(), 0.01);
                        assertEquals(job.signOn(), draft.getSignOnBonus(), 0.01);
                        assertEquals(relocation, draft.getRelocationStipend(), 0.01);
                    }));
        }
        return cases;
    }

    private List<CaseSpec> fteAndPremiumCases() {
        List<CaseSpec> cases = new ArrayList<>();
        PremiumJob[] jobs = {
                new PremiumJob("Seattle WA", "seattle-wa", 60.00, "0.9 FTE", 36.0, 6.0, 4.0, 10000, 5000),
                new PremiumJob("Los Angeles CA", "los-angeles-ca", 58.00, "80% FTE", 32.0, 5.8, 3.0, 9000, 4500),
                new PremiumJob("New York City", "new-york-ny", 64.00, "1.0 FTE", 40.0, 8.0, 4.0, 12000, 6000),
                new PremiumJob("Phoenix AZ", "phoenix-az", 48.00, "36 hrs/wk", 36.0, 4.8, 2.4, 7000, 3500)
        };

        for (int i = 0; i < 200; i++) {
            PremiumJob job = jobs[i % jobs.length];
            double expectedNight = job.nightDollars() / job.rate() * 100.0;
            double expectedWeekend = job.weekendDollars() / job.rate() * 100.0;
            String text = String.format(Locale.US,
                    "%s RN job post. Base rate $%.2f/hr. Schedule %s, nights. Night differential $%.2f/hr. Weekend premium $%.2f/hr. Sign-on bonus $%d. Relocation $%d. Nearby: downtown 11 miles, airport 19 miles.",
                    job.cityLabel(), job.rate(), job.fteText(), job.nightDollars(), job.weekendDollars(),
                    job.signOn(), job.relocation());
            cases.add(new CaseSpec("fte-premium-" + i, text,
                    "rate " + job.rate() + ", hours " + job.hours() + ", night diff percent "
                            + expectedNight + ", weekend diff percent " + expectedWeekend,
                    result -> {
                        OfferRiskDraft draft = result.getDraft();
                        assertTrue(result.isParsed());
                        assertEquals(job.citySlug(), draft.getOfferCitySlug());
                        assertEquals(job.rate(), draft.getOfferHourlyRate(), 0.01);
                        assertEquals(job.hours(), draft.getWeeklyHours(), 0.01);
                        assertEquals(expectedNight, draft.getNightDiffPercent(), 0.01);
                        assertEquals(expectedWeekend, draft.getWeekendDiffPercent(), 0.01);
                        assertEquals(job.signOn(), draft.getSignOnBonus(), 0.01);
                        assertEquals(job.relocation(), draft.getRelocationStipend(), 0.01);
                    }));
        }
        return cases;
    }

    private List<CaseSpec> brochureAndRelocationNoiseCases() {
        List<CaseSpec> cases = new ArrayList<>();
        BrochureJob[] jobs = {
                new BrochureJob("Seattle, WA", "seattle-wa", 55.50, 10000, 4000),
                new BrochureJob("Los Angeles, CA", "los-angeles-ca", 58.75, 9000, 5000),
                new BrochureJob("New York City", "new-york-ny", 63.25, 14000, 7000),
                new BrochureJob("Phoenix, AZ", "phoenix-az", 47.90, 7000, 3500)
        };
        String[] brochureBlocks = {
                "Community guide: population 734000; median home $825000; weekend farmers market.",
                "Campus guide: 4 towers, 1200 beds, 18 miles to airport, 3 miles to downtown.",
                "Regional copy mentions Austin, Seattle, Phoenix, Los Angeles, and New York City for relocation planning.",
                "Benefits brochure: medical premium $220/month, parking $110/month, tuition $5250/year."
        };

        for (int i = 0; i < 200; i++) {
            BrochureJob job = jobs[i % jobs.length];
            String text = String.format(Locale.US,
                    "%s\nOPEN ROLE: RN, emergency/critical care.\nPrimary work location: %s.\nBase hourly rate $%.2f/hr, guaranteed 36 hours weekly.\nNight shift premium 10%%. Weekend differential 8%%.\nSign on bonus amount $%d. Moving reimbursement / relocation support $%d.\n%s",
                    brochureBlocks[i % brochureBlocks.length], job.cityLabel(), job.rate(), job.signOn(),
                    job.relocation(), brochureBlocks[(i + 2) % brochureBlocks.length]);
            cases.add(new CaseSpec("brochure-relocation-noise-" + i, text,
                    "primary city " + job.citySlug() + ", base rate " + job.rate()
                            + ", sign-on " + job.signOn() + ", relocation " + job.relocation(),
                    result -> {
                        OfferRiskDraft draft = result.getDraft();
                        assertTrue(result.isParsed());
                        assertEquals(job.citySlug(), draft.getOfferCitySlug());
                        assertEquals(job.rate(), draft.getOfferHourlyRate(), 0.01);
                        assertEquals(36.0, draft.getWeeklyHours(), 0.01);
                        assertEquals(10.0, draft.getNightDiffPercent(), 0.01);
                        assertEquals(8.0, draft.getWeekendDiffPercent(), 0.01);
                        assertEquals(job.signOn(), draft.getSignOnBonus(), 0.01);
                        assertEquals(job.relocation(), draft.getRelocationStipend(), 0.01);
                    }));
        }
        return cases;
    }

    private record Job(String cityLabel,
                       String citySlug,
                       String unit,
                       String rangeText,
                       double expectedRate,
                       double hours,
                       int signOn,
                       int relocation) {
    }

    private record OcrJob(String cityLabel,
                          String citySlug,
                          String unit,
                          double rate,
                          int ftePercent,
                          int signOn,
                          int relocation) {
    }

    private record RangeJob(String cityLabel,
                            String citySlug,
                            String rangeText,
                            double lowRate,
                            int signOn) {
    }

    private record PremiumJob(String cityLabel,
                              String citySlug,
                              double rate,
                              String fteText,
                              double hours,
                              double nightDollars,
                              double weekendDollars,
                              int signOn,
                              int relocation) {
    }

    private record BrochureJob(String cityLabel,
                               String citySlug,
                               double rate,
                               int signOn,
                               int relocation) {
    }

    private record CaseSpec(String name, String text, String expected, ResultAssertion assertion) {
        private void assertResult(OfferTextParseResult result) {
            assertion.assertResult(result);
        }
    }

    @FunctionalInterface
    private interface ResultAssertion {
        void assertResult(OfferTextParseResult result);
    }
}
