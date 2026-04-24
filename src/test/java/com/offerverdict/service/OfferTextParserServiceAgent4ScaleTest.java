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

class OfferTextParserServiceAgent4ScaleTest {

    private static final Path FAILURE_REPORT = Path.of("build", "reports", "agent4-money-field-failures.txt");

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesOneThousandAdversarialMoneySeparationCases() throws IOException {
        List<CaseSpec> cases = buildCases();

        assertEquals(1000, cases.size(), "agent4 corpus size drifted");

        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(0, cases.size()).parallel().forEach(index -> {
            CaseSpec spec = cases.get(index);
            OfferTextParseResult result = parser.parse(spec.text(), "offer_review");
            List<String> caseFailures = spec.failures(result);
            if (!caseFailures.isEmpty()) {
                failures.add(formatFailure(index, spec, result, caseFailures));
            }
        });

        Files.createDirectories(FAILURE_REPORT.getParent());
        Files.writeString(FAILURE_REPORT, String.join("\n\n", failures));

        assertTrue(failures.isEmpty(), "Agent4 money-field separation failures: " + failures.size()
                + ". Full report: " + FAILURE_REPORT.toAbsolutePath()
                + "\n\n" + String.join("\n\n", failures.stream().limit(25).toList()));
    }

    private List<CaseSpec> buildCases() {
        List<CaseSpec> cases = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            cases.add(caseFor(i));
        }
        return cases;
    }

    private CaseSpec caseFor(int index) {
        City current = currentCities()[index % currentCities().length];
        City offer = offerCities()[(index / 2) % offerCities().length];
        Unit unit = units()[(index / 3) % units().length];
        MoneyFormat bonus = bonusFormats()[index % bonusFormats().length];
        MoneyFormat relocation = relocationFormats()[(index / 5) % relocationFormats().length];
        MoneyFormat moving = movingCostFormats()[(index / 7) % movingCostFormats().length];
        InsuranceFormat insurance = insuranceFormats()[(index / 11) % insuranceFormats().length];
        ShiftFormat shift = shiftFormats()[(index / 13) % shiftFormats().length];
        ContractFormat contract = contractFormats()[(index / 17) % contractFormats().length];

        double signOn = 7500 + (index % 8) * 1250;
        double relocationAmount = 2500 + (index % 6) * 750;
        double movingCost = 5500 + (index % 5) * 900;
        double currentInsurance = 125 + (index % 4) * 35;
        double offerInsurance = 215 + (index % 5) * 45;
        int contractMonths = 18 + (index % 4) * 6;
        int plannedStayMonths = index % 200 == 0 ? 9 + (index % 6) * 3 : contractMonths;
        String repaymentStyle = repaymentStyles()[index % repaymentStyles().length];
        String expectedRepaymentStyle = "no".equals(repaymentStyle) ? "none" : repaymentStyle;
        String moneySeparator = index % 100 == 1 ? " / " : "; ";

        String text = switch (index % 10) {
            case 0 -> String.format(Locale.US,
                    "Current RN in %s at $%.0f/hr. Offer: %s %s RN at $%.0f/hr; %s; %s; %s; %s; %s; %s.",
                    current.label(), current.rate(), offer.label(), unit.label(), offer.rate(),
                    bonus.render(signOn), relocation.render(relocationAmount), moving.render(movingCost),
                    insurance.render(currentInsurance, offerInsurance), shift.render(), contract.render(contractMonths,
                            plannedStayMonths, repaymentStyle));
            case 1 -> String.format(Locale.US,
                    "Me now = %s / $%.0f/hr. New role = %s / %s / $%.0f/hr / 36 hours. %s%s%s%s%s%s%s%s%s%s%s.",
                    current.label(), current.rate(), offer.label(), unit.label(), offer.rate(),
                    bonus.render(signOn), moneySeparator, relocation.render(relocationAmount), moneySeparator,
                    moving.render(movingCost), moneySeparator, shift.render(), moneySeparator,
                    insurance.render(currentInsurance, offerInsurance), moneySeparator,
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle));
            case 2 -> String.format(Locale.US,
                    "%s %s offer first: base rate $%.0f/hr, 36 hrs, %s, %s, %s, %s, %s, %s. Currently %s making $%.0f/hr.",
                    offer.label(), unit.label(), offer.rate(), shift.render(), bonus.render(signOn),
                    relocation.render(relocationAmount), moving.render(movingCost),
                    insurance.render(currentInsurance, offerInsurance),
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle),
                    current.label(), current.rate());
            case 3 -> String.format(Locale.US,
                    "Recruiter note (%s): %s. %s. %s. Base $%.0f/hr in %s. My current setup is %s $%.0f/hr. %s. %s. %s.",
                    unit.label(), relocation.render(relocationAmount), bonus.render(signOn),
                    moving.render(movingCost), offer.rate(), offer.label(), current.label(), current.rate(),
                    insurance.render(currentInsurance, offerInsurance),
                    shift.render(),
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle));
            case 4 -> String.format(Locale.US,
                    "I am comparing %s $%.0f/hr to %s %s $%.0f/hr. Money details: %s; %s; %s; %s; %s; %s.",
                    current.label(), current.rate(), offer.label(), unit.label(), offer.rate(),
                    insurance.render(currentInsurance, offerInsurance), shift.render(), relocation.render(relocationAmount),
                    moving.render(movingCost), bonus.render(signOn),
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle));
            case 5 -> String.format(Locale.US,
                    "Current=%s @$%.0f/hr; Offer=%s %s @$%.0f/hr; %s; %s; %s; %s; %s; %s.",
                    current.label(), current.rate(), offer.label(), unit.label(), offer.rate(),
                    shift.render(), insurance.render(currentInsurance, offerInsurance), bonus.render(signOn),
                    relocation.render(relocationAmount), moving.render(movingCost),
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle));
            case 6 -> String.format(Locale.US,
                    "Offer packet for %s %s at $%.0f/hr says %s, %s, %s, %s and %s. Current job is %s at $%.0f/hr. %s.",
                    offer.label(), unit.label(), offer.rate(),
                    bonus.render(signOn), relocation.render(relocationAmount), moving.render(movingCost),
                    insurance.render(currentInsurance, offerInsurance),
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle),
                    current.label(), current.rate(), shift.render());
            case 7 -> String.format(Locale.US,
                    "phone notes: now/%s/$%.0f/hr -> offer/%s/%s/$%.0f/hr/36h; %s; %s; %s; %s; %s; %s",
                    current.label(), current.rate(), offer.label(), unit.label(), offer.rate(),
                    bonus.render(signOn), relocation.render(relocationAmount), moving.render(movingCost),
                    shift.render(), insurance.render(currentInsurance, offerInsurance),
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle));
            case 8 -> String.format(Locale.US,
                    "Current insurance and moving math are confusing. I work in %s at $%.0f/hr. Offer is %s %s at $%.0f/hr. %s. %s. %s. %s. %s. %s.",
                    current.label(), current.rate(), offer.label(), unit.label(), offer.rate(),
                    moving.render(movingCost), insurance.render(currentInsurance, offerInsurance),
                    relocation.render(relocationAmount), bonus.render(signOn), shift.render(),
                    contract.render(contractMonths, plannedStayMonths, repaymentStyle));
            default -> String.format(Locale.US,
                    "%s RN offer in %s: $%.0f/hr, 36 hours. %s; %s; %s; %s; %s; %s. Current city %s, current pay $%.0f/hr.",
                    unit.label(), offer.label(), offer.rate(), contract.render(contractMonths, plannedStayMonths,
                            repaymentStyle), shift.render(), bonus.render(signOn), relocation.render(relocationAmount),
                    moving.render(movingCost), insurance.render(currentInsurance, offerInsurance),
                    current.label(), current.rate());
        };

        return new CaseSpec("agent4-money-" + index, text, new Expected(current.slug(), offer.slug(), current.rate(),
                offer.rate(), signOn, relocationAmount, movingCost, currentInsurance, offerInsurance,
                shift.expectedNightPercent(offer.rate()), shift.expectedWeekendPercent(offer.rate()), contractMonths,
                plannedStayMonths, expectedRepaymentStyle));
    }

    private static String formatFailure(int index, CaseSpec spec, OfferTextParseResult result, List<String> failures) {
        OfferRiskDraft draft = result.getDraft();
        return "CASE " + index + " " + spec.name()
                + "\nINPUT: " + spec.text()
                + "\nEXPECTED: " + spec.expected()
                + "\nACTUAL: currentCity=" + draft.getCurrentCitySlug()
                + ", offerCity=" + draft.getOfferCitySlug()
                + ", currentRate=" + draft.getCurrentHourlyRate()
                + ", offerRate=" + draft.getOfferHourlyRate()
                + ", signOn=" + draft.getSignOnBonus()
                + ", relocation=" + draft.getRelocationStipend()
                + ", movingCost=" + draft.getMovingCost()
                + ", currentInsurance=" + draft.getCurrentMonthlyInsurance()
                + ", offerInsurance=" + draft.getOfferMonthlyInsurance()
                + ", nightDiff=" + draft.getNightDiffPercent()
                + ", weekendDiff=" + draft.getWeekendDiffPercent()
                + ", contractMonths=" + draft.getContractMonths()
                + ", plannedStayMonths=" + draft.getPlannedStayMonths()
                + ", repaymentStyle=" + draft.getRepaymentStyle()
                + "\nFAILURES: " + failures
                + "\nEXTRACTED: " + result.getExtractedFields()
                + "\nMISSING: " + result.getMissingCriticalFields();
    }

    private static City[] currentCities() {
        return new City[] {
                new City("Austin, TX", "austin-tx", 42),
                new City("Phoenix, AZ", "phoenix-az", 44)
        };
    }

    private static City[] offerCities() {
        return new City[] {
                new City("Seattle, WA", "seattle-wa", 60),
                new City("Los Angeles, CA", "los-angeles-ca", 58),
                new City("New York City", "new-york-ny", 64)
        };
    }

    private static Unit[] units() {
        return new Unit[] {
                new Unit("ICU", "icu"),
                new Unit("ED", "ed"),
                new Unit("med surg telemetry", "med_surg")
        };
    }

    private static MoneyFormat[] bonusFormats() {
        return new MoneyFormat[] {
                new MoneyFormat("sign-on bonus $%s"),
                new MoneyFormat("sign on bonus: $%s"),
                new MoneyFormat("SOB=$%s"),
                new MoneyFormat("retention bonus $%s"),
                new MoneyFormat("commencement bonus: $%s"),
                new MoneyFormat("bonus maybe $%s"),
                new MoneyFormat("bonus ($%s)"),
                new MoneyFormat("$%s sign-on bonus")
        };
    }

    private static MoneyFormat[] relocationFormats() {
        return new MoneyFormat[] {
                new MoneyFormat("relocation stipend $%s"),
                new MoneyFormat("relocation assistance: $%s"),
                new MoneyFormat("relo=$%s"),
                new MoneyFormat("moving reimbursement $%s"),
                new MoneyFormat("$%s relocation")
        };
    }

    private static MoneyFormat[] movingCostFormats() {
        return new MoneyFormat[] {
                new MoneyFormat("moving cost estimate $%s"),
                new MoneyFormat("moving costs: $%s"),
                new MoneyFormat("move cost=$%s"),
                new MoneyFormat("move estimate ($%s)")
        };
    }

    private static InsuranceFormat[] insuranceFormats() {
        return new InsuranceFormat[] {
                (current, offer) -> "current insurance $" + money(current) + "/mo, health insurance premium $"
                        + money(offer) + "/mo",
                (current, offer) -> "offer medical premium $" + money(offer) + " monthly, current premium $"
                        + money(current),
                (current, offer) -> "my insurance $" + money(current) + "; employee premium $" + money(offer),
                (current, offer) -> "benefits premium: $" + money(offer) + "; existing insurance $" + money(current),
                (current, offer) -> "current insurance=$" + money(current) + " / offer health insurance=$"
                        + money(offer)
        };
    }

    private static ShiftFormat[] shiftFormats() {
        return new ShiftFormat[] {
                new ShiftFormat("night premium $6/hr, weekend premium $4/hr", 6, 4, false),
                new ShiftFormat("night differential 10%, weekend differential 8%", 10, 8, true),
                new ShiftFormat("night diff: $5.50/hr; weekend diff: $3.25/hr", 5.5, 3.25, false),
                new ShiftFormat("weekend premium $4.50/hr and night shift premium $7/hr", 7, 4.5, false),
                new ShiftFormat("night premium 12%; weekend premium 9%", 12, 9, true)
        };
    }

    private static ContractFormat[] contractFormats() {
        return new ContractFormat[] {
                (contract, planned, repayment) -> contract + "-month commitment, plan to stay " + planned
                        + " months, " + repayment + " repayment",
                (contract, planned, repayment) -> "contract: " + contract + " months; expected stay " + planned
                        + " months; " + repayment + " repayment",
                (contract, planned, repayment) -> "employment term = " + contract + " months / stay for " + planned
                        + " months / " + repayment + " repayment",
                (contract, planned, repayment) -> "service period " + contract + " months, planning to stay "
                        + planned + " months, " + repayment + " repayment",
                (contract, planned, repayment) -> "repayment period " + contract + " months; leave after " + planned
                        + " months; " + repayment + " repayment"
        };
    }

    private static String[] repaymentStyles() {
        return new String[] {"prorated", "full", "no"};
    }

    private static String money(double value) {
        if (value % 1000 == 0) {
            return String.format(Locale.US, "%.0fk", value / 1000);
        }
        if (value >= 1000 && value % 500 == 0) {
            return String.format(Locale.US, "%.1fk", value / 1000);
        }
        if (value >= 1000) {
            return String.format(Locale.US, "%,.0f", value);
        }
        return String.format(Locale.US, "%.0f", value);
    }

    private record City(String label, String slug, double rate) {
    }

    private record Unit(String label, String slug) {
    }

    private record MoneyFormat(String template) {
        private String render(double amount) {
            return String.format(Locale.US, template, money(amount));
        }
    }

    @FunctionalInterface
    private interface InsuranceFormat {
        String render(double currentInsurance, double offerInsurance);
    }

    @FunctionalInterface
    private interface ContractFormat {
        String render(int contractMonths, int plannedStayMonths, String repaymentStyle);
    }

    private record ShiftFormat(String text, double nightValue, double weekendValue, boolean percent) {
        private String render() {
            return text;
        }

        private double expectedNightPercent(double offerHourlyRate) {
            return percent ? nightValue : (nightValue / offerHourlyRate) * 100.0;
        }

        private double expectedWeekendPercent(double offerHourlyRate) {
            return percent ? weekendValue : (weekendValue / offerHourlyRate) * 100.0;
        }
    }

    private record Expected(String currentCitySlug,
                            String offerCitySlug,
                            double currentHourlyRate,
                            double offerHourlyRate,
                            double signOnBonus,
                            double relocationStipend,
                            double movingCost,
                            double currentInsurance,
                            double offerInsurance,
                            double nightDiffPercent,
                            double weekendDiffPercent,
                            int contractMonths,
                            int plannedStayMonths,
                            String repaymentStyle) {
    }

    private record CaseSpec(String name, String text, Expected expected) {
        private List<String> failures(OfferTextParseResult result) {
            OfferRiskDraft draft = result.getDraft();
            List<String> failures = new ArrayList<>();
            expectClose("signOnBonus", expected.signOnBonus(), draft.getSignOnBonus(), failures);
            expectClose("relocationStipend", expected.relocationStipend(), draft.getRelocationStipend(), failures);
            expectClose("movingCost", expected.movingCost(), draft.getMovingCost(), failures);
            expectClose("currentInsurance", expected.currentInsurance(), draft.getCurrentMonthlyInsurance(), failures);
            expectClose("offerInsurance", expected.offerInsurance(), draft.getOfferMonthlyInsurance(), failures);
            expectReasonablePercent("nightDiffPercent", draft.getNightDiffPercent(), failures);
            expectReasonablePercent("weekendDiffPercent", draft.getWeekendDiffPercent(), failures);
            expectEquals("contractMonths", expected.contractMonths(), draft.getContractMonths(), failures);
            expectEquals("plannedStayMonths", expected.plannedStayMonths(), draft.getPlannedStayMonths(), failures);
            expectEquals("repaymentStyle", expected.repaymentStyle(), draft.getRepaymentStyle(), failures);
            return failures;
        }

        private void expectClose(String field, double expectedValue, double actualValue, List<String> failures) {
            if (Math.abs(expectedValue - actualValue) > 0.02) {
                failures.add(field + " expected " + expectedValue + " but was " + actualValue);
            }
        }

        private void expectEquals(String field, Object expectedValue, Object actualValue, List<String> failures) {
            if ((expectedValue == null && actualValue != null)
                    || (expectedValue != null && !expectedValue.equals(actualValue))) {
                failures.add(field + " expected " + expectedValue + " but was " + actualValue);
            }
        }

        private void expectReasonablePercent(String field, double actualValue, List<String> failures) {
            if (actualValue <= 0 || actualValue >= 25) {
                failures.add(field + " expected a positive premium below 25% but was " + actualValue);
            }
        }
    }
}
