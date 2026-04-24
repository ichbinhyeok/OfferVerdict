package com.offerverdict.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.offerverdict.data.DataRepository;
import com.offerverdict.model.OfferTextParseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferTextParserServiceNaturalLanguageTest {

    private final OfferTextParserService parser = new OfferTextParserService(repository());

    private static DataRepository repository() {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DataRepository repository = new DataRepository(objectMapper);
        repository.reload();
        return repository;
    }

    @Test
    void parsesPlainEnglishOfferConcern() {
        OfferTextParseResult result = parser.parse(
                "I am an RN in Austin making $42/hr. I got a Seattle ICU offer at $60/hr for 36 hours, "
                        + "nights, $15k sign-on, $4k relocation, 24-month commitment. I am worried about hospital-wide "
                        + "float and whether hours are guaranteed.",
                "offer_review");

        assertEquals("austin-tx", result.getDraft().getCurrentCitySlug());
        assertEquals("seattle-wa", result.getDraft().getOfferCitySlug());
        assertEquals(42.0, result.getDraft().getCurrentHourlyRate());
        assertEquals(60.0, result.getDraft().getOfferHourlyRate());
        assertEquals(36.0, result.getDraft().getWeeklyHours());
        assertEquals(15000.0, result.getDraft().getSignOnBonus());
        assertEquals(4000.0, result.getDraft().getRelocationStipend());
        assertEquals(24, result.getDraft().getContractMonths());
        assertEquals("icu", result.getDraft().getUnitType());
        assertEquals("hospital_wide", result.getDraft().getFloatRisk());
        assertTrue(result.getMissingCriticalFields().contains("Moving cost estimate"));
    }

    @Test
    void keepsSignOnAndRelocationAmountsSeparateWhenBothAppear() {
        OfferTextParseResult result = parser.parse(
                "Current job: RN in Austin, TX at $42/hr. New ICU RN offer in Seattle, WA at $60/hr. "
                        + "Sign-on bonus $15000. Relocation stipend $4000. "
                        + "24 month commitment with prorated repayment. Hospital-wide float.",
                "offer_review");

        assertEquals(15000.0, result.getDraft().getSignOnBonus());
        assertEquals(4000.0, result.getDraft().getRelocationStipend());
        assertTrue(result.getExtractedFields().contains("Sign-on bonus: $15,000"));
        assertTrue(result.getExtractedFields().contains("Relocation support: $4,000"));
    }

    @Test
    void parsesRecruiterStyleMessage() {
        OfferTextParseResult result = parser.parse(
                "Recruiter says the new ED RN position is located in Los Angeles, CA. Base rate is $58/hr, "
                        + "three 12s, night shift, $7500 sign on bonus. I currently work in Phoenix, AZ at $44/hr. "
                        + "They said float is within the service line but the shift is based on staffing needs.",
                "offer_review");

        assertEquals("phoenix-az", result.getDraft().getCurrentCitySlug());
        assertEquals("los-angeles-ca", result.getDraft().getOfferCitySlug());
        assertEquals(44.0, result.getDraft().getCurrentHourlyRate());
        assertEquals(58.0, result.getDraft().getOfferHourlyRate());
        assertEquals(36.0, result.getDraft().getWeeklyHours());
        assertEquals(7500.0, result.getDraft().getSignOnBonus());
        assertEquals("ed", result.getDraft().getUnitType());
        assertEquals("adjacent_units", result.getDraft().getFloatRisk());
        assertEquals("unknown", result.getDraft().getShiftGuarantee());
    }

    @Test
    void parsesJobPostLikeNaturalLanguageScreen() {
        OfferTextParseResult result = parser.parse(
                "Thinking about applying to a med surg telemetry RN job in Seattle, WA. "
                        + "Posted pay range is $49.23 to $91.22 per hour, 90% FTE nights, "
                        + "weekend premium $4/hr, up to $10000 sign-on bonus. Float policy is not listed.",
                "job_post");

        assertEquals("seattle-wa", result.getDraft().getOfferCitySlug());
        assertEquals(49.23, result.getDraft().getOfferHourlyRate());
        assertEquals(36.0, result.getDraft().getWeeklyHours());
        assertEquals(10000.0, result.getDraft().getSignOnBonus());
        assertEquals("med_surg", result.getDraft().getUnitType());
        assertTrue(result.getExtractedFields().stream().anyMatch(field -> field.contains("Posted pay range")));
    }

    @Test
    void returnsMissingFieldsWhenUserOnlyDescribesAnxiety() {
        OfferTextParseResult result = parser.parse(
                "I got an ICU nurse offer and the bonus sounds good, but I am nervous about floating and being cancelled.",
                "offer_review");

        assertTrue(result.isParsed());
        assertEquals("icu", result.getDraft().getUnitType());
        assertTrue(result.getMissingCriticalFields().contains("Offer city"));
        assertTrue(result.getMissingCriticalFields().contains("Offer hourly rate"));
        assertTrue(result.getMissingCriticalFields().contains("Current city"));
        assertTrue(result.getMissingCriticalFields().contains("Current hourly rate"));
    }

    @Test
    void treatsCultureConcernAsSignalButRequiresBasicsBeforeVerdict() {
        OfferTextParseResult result = parser.parse(
                "I got an offer in New York City, but I heard the unit has toxic culture and bullying. "
                        + "I am worried I will regret signing.",
                "offer_review");

        assertTrue(result.isParsed());
        assertEquals("new-york-ny", result.getDraft().getOfferCitySlug());
        assertTrue(result.getExtractedFields().contains("Concern: unit culture / bullying risk"));
        assertTrue(result.getMissingCriticalFields().contains("Offer hourly rate"));
        assertTrue(result.getMissingCriticalFields().contains("Current city"));
        assertTrue(result.getSummary().contains("not enough for a final verdict"));
        assertTrue(result.getParseWarning().contains("missing basics"));
    }

    @Test
    void keepsPositiveCoworkerTradeoffWithoutPretendingItCanCalculate() {
        OfferTextParseResult result = parser.parse(
                "I got an offer but the income is lower. The coworkers are good and the manager seems good, "
                        + "so I am torn.",
                "offer_review");

        assertTrue(result.isParsed());
        assertTrue(result.getExtractedFields().contains("Concern: lower take-home pay"));
        assertTrue(result.getExtractedFields().contains("Positive tradeoff: team / support seems strong"));
        assertTrue(result.getMissingCriticalFields().contains("Offer city"));
        assertTrue(result.getMissingCriticalFields().contains("Offer hourly rate"));
        assertTrue(result.getSummary().contains("not enough for a final verdict"));
    }
}
