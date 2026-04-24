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

class OfferTextParserServiceAgent1ScaleTest {

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandOfferFirstReversedOrderMobileShorthandInputs() {
        List<CaseSpec> cases = agentOneCases();

        assertEquals(1000, cases.size(), "agent 1 corpus size drifted");
        assertCasesPass(cases);
    }

    private void assertCasesPass(List<CaseSpec> cases) {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(i -> {
            CaseSpec spec = cases.get(i);
            OfferTextParseResult result = parser.parse(spec.text(), "offer_review");
            List<String> mismatches = mismatches(spec, result);
            if (!mismatches.isEmpty()) {
                failures.add("case " + i + " [" + spec.name() + "]"
                        + "\nINPUT: " + spec.text()
                        + "\nEXPECTED: " + spec.expectedSummary()
                        + "\nACTUAL: " + actualSummary(result.getDraft())
                        + "\nMISMATCHES: " + mismatches
                        + "\nEXTRACTED: " + result.getExtractedFields()
                        + "\nMISSING: " + result.getMissingCriticalFields()
                        + "\nSUMMARY: " + result.getSummary());
            }
        });

        assertTrue(failures.isEmpty(), String.join("\n\n", failures));
    }

    private List<CaseSpec> agentOneCases() {
        List<CaseSpec> cases = new ArrayList<>();
        City[] cities = {
                new City("Seattle WA", "seattle-wa", 60),
                new City("LA", "los-angeles-ca", 58),
                new City("NYC", "new-york-ny", 64),
                new City("Phoenix AZ", "phoenix-az", 52),
                new City("Austin TX", "austin-tx", 49)
        };
        Unit[] units = {
                new Unit("ICU,", "icu"),
                new Unit("ED.", "ed"),
                new Unit("med surg tele", "med_surg")
        };

        for (int i = 0; i < 1000; i++) {
            City offer = cities[i % cities.length];
            City current = cities[(i + 2) % cities.length];
            Unit unit = units[i % units.length];
            int signOn = 8000 + (i % 8) * 1000;
            int relocation = 2500 + (i % 6) * 500;
            cases.add(new CaseSpec(
                    "agent1-reversed-mobile-" + i,
                    textFor(i % 10, offer, current, unit, signOn, relocation),
                    current.slug(),
                    offer.slug(),
                    current.rate(),
                    offer.rate(),
                    unit.slug(),
                    signOn,
                    relocation));
        }
        return cases;
    }

    private String textFor(int template, City offer, City current, Unit unit, int signOn, int relocation) {
        return switch (template) {
            case 0 -> String.format(Locale.US,
                    "Offer first: %s %s RN role at $%.0f/hr, 36h, nights, sign-on $%d, relo $%d, 24 month commitment. Current job after this: %s RN at $%.0f/hr.",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
            case 1 -> String.format(Locale.US,
                    "NEW THING %s / %s-RN / base rate $%.0f/hr / 36 hrs/wk / SOB %s / RELO $%d. ME NOW %s / $%.0f/hr.",
                    offer.label(), unit.label(), offer.rate(), k(signOn), relocation, current.label(), current.rate());
            case 2 -> String.format(Locale.US,
                    "offer -> %s -> %s rn -> $%.0f/hr -> 36h -> sign on %s -> relocation $%d ; current -> %s -> $%.0f/hr",
                    offer.label(), unit.label(), offer.rate(), k(signOn), relocation, current.label(), current.rate());
            case 3 -> String.format(Locale.US,
                    "offer in %s %s rn rate $%.0f per hour 36 hours nights sign on bonus $%d relocation stipend $%d current in %s making $%.0f per hour",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
            case 4 -> String.format(Locale.US,
                    "oFfEr %s %s, RN POSITION, $%.0f/hour, gtd 36 hrs, sob $%d, relo $%d. cUrReNt %s existing RN $%.0f/hour",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
            case 5 -> String.format(Locale.US,
                    "%s RN - %s offer details: base rate $%.0f/hr; 3x12; commencement bonus $%d; moving reimbursement $%d. after that my current is %s, $%.0f/hr.",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
            case 6 -> String.format(Locale.US,
                    "new role/%s/%s/$%.0f/hr/36h/noc/sob %s/relo $%d%nnow/%s/$%.0f/hr",
                    offer.label(), unit.label(), offer.rate(), k(signOn), relocation, current.label(), current.rate());
            case 7 -> String.format(Locale.US,
                    "Got the %s %s offer at $%.0f/hr with 36 hrs/wk and $%d bonus + $%d relocation. I would be coming from %s where I make $%.0f/hr now.",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
            case 8 -> String.format(Locale.US,
                    "Recruiter text: position located in %s for %s RN, base rate $%.0f/hr, sign-on bonus $%d, relocation assistance $%d, 36 hours. My current job is in %s at $%.0f/hr.",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
            default -> String.format(Locale.US,
                    "OFFER %s %s RN $%.0f/hr 36h SOB $%d RELO $%d CURRENT %s RN $%.0f/hr",
                    offer.label(), unit.label(), offer.rate(), signOn, relocation, current.label(), current.rate());
        };
    }

    private List<String> mismatches(CaseSpec spec, OfferTextParseResult result) {
        OfferRiskDraft draft = result.getDraft();
        List<String> mismatches = new ArrayList<>();
        requireEquals(mismatches, "currentCitySlug", spec.currentCitySlug(), draft.getCurrentCitySlug());
        requireEquals(mismatches, "offerCitySlug", spec.offerCitySlug(), draft.getOfferCitySlug());
        requireClose(mismatches, "currentHourlyRate", spec.currentHourlyRate(), draft.getCurrentHourlyRate());
        requireClose(mismatches, "offerHourlyRate", spec.offerHourlyRate(), draft.getOfferHourlyRate());
        requireClose(mismatches, "weeklyHours", 36.0, draft.getWeeklyHours());
        requireEquals(mismatches, "unitType", spec.unitType(), draft.getUnitType());
        requireClose(mismatches, "signOnBonus", spec.signOnBonus(), draft.getSignOnBonus());
        requireClose(mismatches, "relocationStipend", spec.relocationStipend(), draft.getRelocationStipend());
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

    private String actualSummary(OfferRiskDraft draft) {
        return "currentCitySlug=" + draft.getCurrentCitySlug()
                + ", offerCitySlug=" + draft.getOfferCitySlug()
                + ", currentHourlyRate=" + draft.getCurrentHourlyRate()
                + ", offerHourlyRate=" + draft.getOfferHourlyRate()
                + ", weeklyHours=" + draft.getWeeklyHours()
                + ", unitType=" + draft.getUnitType()
                + ", signOnBonus=" + draft.getSignOnBonus()
                + ", relocationStipend=" + draft.getRelocationStipend();
    }

    private String k(int value) {
        return (value / 1000) + "k";
    }

    private record City(String label, String slug, double rate) {
    }

    private record Unit(String label, String slug) {
    }

    private record CaseSpec(String name,
                            String text,
                            String currentCitySlug,
                            String offerCitySlug,
                            double currentHourlyRate,
                            double offerHourlyRate,
                            String unitType,
                            double signOnBonus,
                            double relocationStipend) {

        private String expectedSummary() {
            return "currentCitySlug=" + currentCitySlug
                    + ", offerCitySlug=" + offerCitySlug
                    + ", currentHourlyRate=" + currentHourlyRate
                    + ", offerHourlyRate=" + offerHourlyRate
                    + ", weeklyHours=36.0"
                    + ", unitType=" + unitType
                    + ", signOnBonus=" + signOnBonus
                    + ", relocationStipend=" + relocationStipend;
        }
    }
}
