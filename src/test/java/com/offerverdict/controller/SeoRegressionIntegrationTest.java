package com.offerverdict.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeoRegressionIntegrationTest {

    @LocalServerPort
    int port;

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Test
    void home_isV2OfferRiskToolAndIndexable() throws Exception {
        HttpResponse<String> response = httpGet("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Paste the RN offer that is making you hesitate."));
        assertTrue(response.body().contains("For nurses with an offer in hand"));
        assertTrue(response.body().contains("Review an offer letter"));
        assertTrue(response.body().contains("Only have a job post?"));
        assertTrue(response.body().contains("action=\"/offer-risk-report\""));
        assertTrue(response.body().contains("action=\"/offer-risk-draft\""));
        assertTrue(response.body().contains("name=\"robots\" content=\"index, follow\""));
        assertTrue(response.body().contains("Offer intake"));
        assertTrue(response.body().contains("No PDF required"));
        assertTrue(response.body().contains("name=\"sourceFile\""));
        assertTrue(response.body().contains("PDF, text files, screenshots, and image OCR work now."));
        assertTrue(response.body().contains("Current baseline and written offer target"));
        assertTrue(response.body().contains("No clean letter yet? Confirm only the terms the tool could not read."));
        assertTrue(response.body().contains("Should this listing even get your time?"));
        assertTrue(response.body().contains("Keep the wedge narrow. Keep the exposure surface broad."));
        assertTrue(response.body().contains("Different RN units fail in different ways."));
        assertTrue(response.body().contains("href=\"/rn-offer-red-flags\""));
        assertTrue(response.body().contains("href=\"/should-i-accept-nurse-job-offer\""));
        assertTrue(response.body().contains("href=\"/should-i-sign-nurse-job-offer\""));
        assertTrue(response.body().contains("href=\"/nurse-offer-life-fit\""));
        assertTrue(response.body().contains("href=\"/nurse-offer-negotiation-questions\""));
        assertTrue(response.body().contains("href=\"/travel-nurse-contract-red-flags\""));
        assertFalse(response.body().contains("Salary Reality Check"));
        assertFalse(response.body().contains("/job/software-engineer"));
    }

    @Test
    void offerRiskReport_rendersDecisionAndContractRisk() throws Exception {
        HttpResponse<String> response = httpGet(
                "/offer-risk-report?currentCitySlug=austin-tx&offerCitySlug=seattle-wa"
                        + "&roleSlug=registered-nurse&currentHourlyRate=42&offerHourlyRate=56"
                        + "&weeklyHours=36&overtimeHours=4&nightDiffPercent=12&nightHours=18"
                        + "&weekendDiffPercent=8&weekendHours=8&unitType=icu"
                        + "&shiftGuarantee=unknown&floatRisk=hospital_wide&cancelRisk=can_cancel_without_pay"
                        + "&currentMonthlyInsurance=150&offerMonthlyInsurance=500&signOnBonus=15000"
                        + "&relocationStipend=5000&movingCost=7000&contractMonths=24"
                        + "&plannedStayMonths=12&repaymentStyle=prorated");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Offer Review"));
        assertTrue(response.body().contains("Current to offer"));
        assertTrue(response.body().contains("Repayment exposure"));
        assertTrue(response.body().contains("Nurse schedule risk"));
        assertTrue(response.body().contains("Why this verdict"));
        assertTrue(response.body().contains("What could still change this"));
        assertTrue(response.body().contains("Do not sign until"));
        assertTrue(response.body().contains("Biggest risks in this packet"));
        assertTrue(response.body().contains("Can you survive this ICU / critical care offer?"));
        assertTrue(response.body().contains("Walk-away line"));
        assertTrue(response.body().contains("Copy this negotiation note"));
        assertTrue(response.body().contains("What would help next?"));
        assertTrue(response.body().contains("I would want a second review"));
        assertTrue(response.body().contains("Written terms on paper"));
        assertTrue(response.body().contains("Hospital-wide float"));
        assertTrue(response.body().contains("Ask these before you sign"));
        assertTrue(response.body().contains("How to salvage the offer"));
        assertTrue(response.body().contains("Full HR question bank"));
        assertTrue(response.body().contains("name=\"robots\" content=\"noindex, follow\""));
    }

    @Test
    void postedOfferRiskReport_carriesDocumentEvidenceIntoVerdictPage() throws Exception {
        String body = "analysisMode=offer_review"
                + "&roleSlug=registered-nurse&currentCitySlug=austin-tx&offerCitySlug=seattle-wa"
                + "&unitType=icu&shiftGuarantee=unknown&floatRisk=hospital_wide&cancelRisk=can_cancel_without_pay"
                + "&currentHourlyRate=42&offerHourlyRate=60&weeklyHours=36"
                + "&signOnBonus=15000&relocationStipend=4000&movingCost=7000"
                + "&contractMonths=24&plannedStayMonths=12&repaymentStyle=prorated"
                + "&documentSourceLabel=" + encode("Uploaded PDF: offer-letter.pdf")
                + "&sourceText=" + encode("Current job: RN in Austin, TX at $42/hr.\n"
                + "New ICU RN offer in Seattle, WA at $60/hr.\n"
                + "Sign-on bonus $15000.\n"
                + "24 month commitment with prorated repayment.\n"
                + "Hospital-wide float and can cancel without pay.");
        HttpResponse<String> response = httpPostForm("/offer-risk-report", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Evidence from your input"));
        assertTrue(response.body().contains("Uploaded PDF: offer-letter.pdf"));
        assertTrue(response.body().contains("Sign-on bonus $15000"));
        assertTrue(response.body().contains("Hospital-wide float"));
        assertTrue(response.body().contains("A PDF is optional"));
    }

    @Test
    void jobPostReport_usesLighterScreenLanguage() throws Exception {
        HttpResponse<String> response = httpGet(
                "/offer-risk-report?analysisMode=job_post&offerCitySlug=seattle-wa"
                        + "&roleSlug=registered-nurse&offerHourlyRate=52&weeklyHours=36"
                        + "&unitType=icu&shiftGuarantee=unknown&floatRisk=hospital_wide"
                        + "&cancelRisk=unknown&contractMonths=0&signOnBonus=0&relocationStipend=0"
                        + "&movingCost=0&offerMonthlyInsurance=0");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Job Post Screen"));
        assertTrue(response.body().contains("Listing focus"));
        assertTrue(response.body().contains("Estimated leftover in city"));
        assertTrue(response.body().contains("Why this verdict"));
        assertTrue(response.body().contains("What could still change this"));
        assertTrue(response.body().contains("Before you spend time"));
        assertTrue(response.body().contains("Ask these before you spend more time"));
        assertTrue(response.body().contains("Walk-away line"));
        assertTrue(response.body().contains("Copy this recruiter message"));
        assertTrue(response.body().contains("What would help next?"));
        assertTrue(response.body().contains("Show me better RN offers"));
        assertTrue(response.body().contains("Everything else flagged"));
        assertTrue(response.body().contains("Full recruiter question bank"));
        assertTrue(response.body().contains("How to salvage the screen"));
        assertFalse(response.body().contains("Questions to ask HR"));
        assertFalse(response.body().contains("Negotiation moves"));
    }

    @Test
    void rnIssuePages_areIndexableAndBridgeToOfferReview() throws Exception {
        HttpResponse<String> hub = httpGet("/rn-offer-red-flags");
        HttpResponse<String> clawback = httpGet("/rn-sign-on-bonus-clawback");
        HttpResponse<String> icu = httpGet("/icu-nurse-offer-red-flags");
        HttpResponse<String> accept = httpGet("/should-i-accept-nurse-job-offer");
        HttpResponse<String> sign = httpGet("/should-i-sign-nurse-job-offer");
        HttpResponse<String> lifeFit = httpGet("/nurse-offer-life-fit");
        HttpResponse<String> travel = httpGet("/travel-nurse-contract-red-flags");

        assertEquals(200, hub.statusCode());
        assertTrue(hub.body().contains("RN offer red flags before signing"));
        assertTrue(hub.body().contains("Same wedge, more entry points."));
        assertTrue(hub.body().contains("This page is the preflight. The verdict comes from the offer review."));
        assertTrue(hub.body().contains("No PDF required."));
        assertTrue(hub.body().contains("What changes the decision"));
        assertTrue(hub.body().contains("Better written offer language looks like this"));
        assertTrue(hub.body().contains("Negotiation asks to send back"));
        assertTrue(hub.body().contains("href=\"/should-i-accept-nurse-job-offer\""));
        assertTrue(hub.body().contains("href=\"/nurse-offer-letter-red-flags\""));
        assertTrue(hub.body().contains("name=\"robots\" content=\"index, follow\""));

        assertEquals(200, clawback.statusCode());
        assertTrue(clawback.body().contains("RN sign-on bonus clawback"));
        assertTrue(clawback.body().contains("Do not let a bonus hide the downside."));
        assertTrue(clawback.body().contains("Paste your offer letter"));
        assertTrue(clawback.body().contains("href=\"/nurse-relocation-offer-checker\""));
        assertTrue(clawback.body().contains("name=\"robots\" content=\"index, follow\""));

        assertEquals(200, icu.statusCode());
        assertTrue(icu.body().contains("ICU nurse offer red flags"));
        assertTrue(icu.body().contains("ICU pay only works if the support matches the acuity."));
        assertTrue(icu.body().contains("How long is ICU orientation?"));
        assertTrue(icu.body().contains("Run the offer review"));
        assertTrue(icu.body().contains("name=\"robots\" content=\"index, follow\""));

        assertEquals(200, accept.statusCode());
        assertTrue(accept.body().contains("Should I accept this nurse job offer?"));
        assertTrue(accept.body().contains("accept, negotiate, or walk away"));

        assertEquals(200, sign.statusCode());
        assertTrue(sign.body().contains("Should I sign this nurse job offer?"));
        assertTrue(sign.body().contains("signing hesitation"));

        assertEquals(200, lifeFit.statusCode());
        assertTrue(lifeFit.body().contains("Nurse offer life fit review"));
        assertTrue(lifeFit.body().contains("A better hourly rate can still be the wrong weekly life."));

        assertEquals(200, travel.statusCode());
        assertTrue(travel.body().contains("Travel nurse contract red flags"));
        assertTrue(travel.body().contains("Guaranteed hours"));
    }

    @Test
    void generatedHighIntentRnPages_areIndexableAndUnitSpecific() throws Exception {
        HttpResponse<String> icuHours = httpGet("/icu-rn-guaranteed-hours");
        HttpResponse<String> edFloat = httpGet("/ed-rn-float-policy");
        HttpResponse<String> medSurgStaffing = httpGet("/med-surg-tele-rn-staffing-ratio");
        HttpResponse<String> laborOrientation = httpGet("/labor-delivery-rn-orientation-length");

        assertEquals(200, icuHours.statusCode());
        assertTrue(icuHours.body().contains("ICU RN guaranteed hours"));
        assertTrue(icuHours.body().contains("high acuity, vasoactive drips"));
        assertTrue(icuHours.body().contains("Are weekly hours guaranteed in writing?"));
        assertTrue(icuHours.body().contains("Better written offer language looks like this"));
        assertTrue(icuHours.body().contains("Weak language to challenge"));
        assertTrue(icuHours.body().contains("href=\"/icu-rn-sign-on-bonus-clawback\""));
        assertTrue(icuHours.body().contains("href=\"/ed-rn-guaranteed-hours\""));
        assertTrue(icuHours.body().contains("href=\"/nurse-offer-negotiation-questions\""));
        assertTrue(icuHours.body().contains("name=\"robots\" content=\"index, follow\""));

        assertEquals(200, edFloat.statusCode());
        assertTrue(edFloat.body().contains("ED RN float policy"));
        assertTrue(edFloat.body().contains("boarding, hallway care, psych holds"));
        assertTrue(edFloat.body().contains("Which units and campuses are included?"));

        assertEquals(200, medSurgStaffing.statusCode());
        assertTrue(medSurgStaffing.body().contains("Med-surg tele RN staffing ratio"));
        assertTrue(medSurgStaffing.body().contains("high patient turnover"));
        assertTrue(medSurgStaffing.body().contains("What is the typical assignment and acuity mix?"));

        assertEquals(200, laborOrientation.statusCode());
        assertTrue(laborOrientation.body().contains("Labor and delivery RN orientation length"));
        assertTrue(laborOrientation.body().contains("induction volume"));
        assertTrue(laborOrientation.body().contains("How many weeks are guaranteed?"));
    }

    @Test
    void issueLandingContextCarriesIntoToolDraftAndReport() throws Exception {
        HttpResponse<String> tool = httpGet("/nurse-relocation-offer-checker?issue=icu-rn-guaranteed-hours");

        assertEquals(200, tool.statusCode());
        assertTrue(tool.body().contains("review focus from landing page"));
        assertTrue(tool.body().contains("ICU RN guaranteed hours"));
        assertTrue(tool.body().contains("name=\"issue\" value=\"icu-rn-guaranteed-hours\""));

        String draftBody = "variant=/nurse-relocation-offer-checker&analysisMode=offer_review"
                + "&issue=icu-rn-guaranteed-hours&sourceText="
                + encode("ICU RN offer in Seattle, WA at $60/hr. Guaranteed 36 hours per week. "
                        + "Hospital-wide float. Sign-on bonus $15000.");
        HttpResponse<String> draft = httpPostForm("/offer-risk-draft", draftBody);

        assertEquals(200, draft.statusCode());
        assertTrue(draft.body().contains("ICU RN guaranteed hours"));
        assertTrue(draft.body().contains("name=\"issue\" value=\"icu-rn-guaranteed-hours\""));
        assertTrue(draft.body().contains("Auto-filled"));

        String reportBody = "analysisMode=offer_review&issue=icu-rn-guaranteed-hours"
                + "&roleSlug=registered-nurse&currentCitySlug=austin-tx&offerCitySlug=seattle-wa"
                + "&unitType=icu&shiftGuarantee=written&floatRisk=hospital_wide&cancelRisk=protected_hours"
                + "&currentHourlyRate=42&offerHourlyRate=60&weeklyHours=36"
                + "&signOnBonus=15000&relocationStipend=4000&movingCost=7000"
                + "&contractMonths=24&plannedStayMonths=12&repaymentStyle=prorated"
                + "&sourceText=" + encode("ICU RN offer in Seattle with guaranteed 36 hours.");
        HttpResponse<String> report = httpPostForm("/offer-risk-report", reportBody);

        assertEquals(200, report.statusCode());
        assertTrue(report.body().contains("Review focus from the page you entered"));
        assertTrue(report.body().contains("ICU RN guaranteed hours"));
        assertTrue(report.body().contains("Are weekly hours guaranteed in writing?"));
    }

    @Test
    void contact_canRenderIntentPresetFromReportContext() throws Exception {
        HttpResponse<String> response = httpGet(
                "/contact?intent=scan_ocr_feedback&analysisMode=offer_review&verdict=NEGOTIATE"
                        + "&role=" + encode("Registered Nurse (RN)")
                        + "&city=" + encode("Seattle, WA"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("I would want scan OCR to be more reliable on messy offer pages."));
        assertTrue(response.body().contains("Scan OCR feedback"));
        assertTrue(response.body().contains("Offer review"));
        assertTrue(response.body().contains("NEGOTIATE"));
        assertTrue(response.body().contains("Seattle, WA"));
        assertTrue(response.body().contains("Open prefilled email"));
    }

    @Test
    void pastedOfferDraft_prefillsDetectedFields() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&sourceText="
                + encode("Current job: RN in Austin, TX at $42/hr. New ICU RN offer in Seattle, WA at $60/hr."
                + " Sign-on bonus $15000. Relocation stipend $4000. 24 month commitment."
                + " Night shift 7p-7a. Hospital-wide float. Can cancel without pay.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Auto-filled"));
        assertTrue(response.body().contains("Offer city: Seattle, WA"));
        assertTrue(response.body().contains("Current city: Austin, TX"));
        assertTrue(response.body().contains("Sign-on bonus: $15,000"));
        assertTrue(response.body().contains("Pasted offer text"));
        assertTrue(response.body().contains("Check what we understood before the verdict."));
        assertTrue(response.body().contains("We understood"));
        assertTrue(response.body().contains("Next step"));
        assertTrue(response.body().contains("Input evidence we used"));
        assertTrue(response.body().contains("name=\"offerHourlyRate\""));
        assertTrue(response.body().contains("value=\"60.0\""));
        assertTrue(response.body().contains("Hospital-wide float"));
    }

    @Test
    void informalOfferConcernWithoutPdf_prefillsAndReportsUsefulChecklist() throws Exception {
        String concern = "I am an RN in Austin making $42/hr. I got a Seattle ICU offer at $60/hr for 36 hours, "
                + "nights, $15k sign-on, $4k relocation, 24-month commitment. I am worried about hospital-wide "
                + "float and whether hours are guaranteed.";
        String draftBody = "variant=/nurse-relocation-offer-checker&analysisMode=offer_review&sourceText="
                + encode(concern);
        HttpResponse<String> draft = httpPostForm("/offer-risk-draft", draftBody);

        assertEquals(200, draft.statusCode());
        assertTrue(draft.body().contains("Auto-filled"));
        assertTrue(draft.body().contains("Pasted offer text"));
        assertTrue(draft.body().contains("Check what we understood before the verdict."));
        assertTrue(draft.body().contains("Needs a few facts"));
        assertTrue(draft.body().contains("Offer city: Seattle, WA"));
        assertTrue(draft.body().contains("Current city: Austin, TX"));
        assertTrue(draft.body().contains("Offer hourly rate: $60/hr"));
        assertTrue(draft.body().contains("Current hourly rate: $42/hr"));
        assertTrue(draft.body().contains("Sign-on bonus: $15,000"));
        assertTrue(draft.body().contains("Input evidence we used"));
        assertTrue(draft.body().contains("No clean letter yet? Confirm only the terms the tool could not read."));

        String reportBody = "analysisMode=offer_review"
                + "&roleSlug=registered-nurse&currentCitySlug=austin-tx&offerCitySlug=seattle-wa"
                + "&unitType=icu&shiftGuarantee=unknown&floatRisk=hospital_wide&cancelRisk=unknown"
                + "&currentHourlyRate=42&offerHourlyRate=60&weeklyHours=36"
                + "&signOnBonus=15000&relocationStipend=4000&movingCost=0"
                + "&contractMonths=24&plannedStayMonths=12&repaymentStyle=prorated"
                + "&sourceText=" + encode(concern);
        HttpResponse<String> report = httpPostForm("/offer-risk-report", reportBody);

        assertEquals(200, report.statusCode());
        assertTrue(report.body().contains("Evidence from your input"));
        assertTrue(report.body().contains("Pasted offer text"));
        assertTrue(report.body().contains("A PDF is optional"));
        assertTrue(report.body().contains("Confirm before you sign"));
        assertTrue(report.body().contains("Moving cost estimate"));
        assertTrue(report.body().contains("Ask these before you sign"));
    }

    @Test
    void lowInformationConcernShowsIntakeGateInsteadOfPretendingVerdictIsReady() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=offer_review&sourceText="
                + encode("I got an offer in New York City, but I heard the unit has toxic culture and bullying. "
                + "I am worried I will regret signing.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Check what we understood before the verdict."));
        assertTrue(response.body().contains("Needs a few facts"));
        assertTrue(response.body().contains("Offer city: New York, NY"));
        assertTrue(response.body().contains("Concern: unit culture / bullying risk"));
        assertTrue(response.body().contains("Needed for a real verdict"));
        assertTrue(response.body().contains("Offer hourly rate"));
        assertTrue(response.body().contains("Current city"));
        assertTrue(response.body().contains("Do not trust the verdict yet."));
    }

    @Test
    void pastedJobPostDraft_usesConservativeFloorForPostedPayRange() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("Seattle, WA Registered Nurse 3 - Medicine/Telemetry. "
                + "$49.23 - $91.22 per hour. 100% FTE, 40 hours per week. "
                + "Up to $10,000 sign-on bonus.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Auto-filled"));
        assertTrue(response.body().contains("Posted pay range: $49.23 - $91.22/hr (using floor $49.23/hr)"));
        assertTrue(response.body().contains("Job-post screens use the conservative floor until a written offer locks the actual rate."));
        assertTrue(response.body().contains("Offer city: Seattle, WA"));
        assertTrue(response.body().contains("value=\"49.23\""));
        assertFalse(response.body().contains("value=\"91.22\""));
    }

    @Test
    void pastedJobPostDraft_convertsPercentFteToWeeklyHours() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("Harborview Medical Center 3EH Medicine/Telemetry RN. "
                + "90% FTE Night Shift in Seattle, WA. "
                + "Pay Range Minimum: $45.59 hourly Pay Range Maximum $84.47 hourly. "
                + "Night Shift Premium - $5.00/hour Weekend Shift Premium - $4.00/hour.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Scheduled hours: 36/week"));
        assertTrue(response.body().contains("value=\"36.0\""));
        assertTrue(response.body().contains("Posted pay range: $45.59 - $84.47/hr (using floor $45.59/hr)"));
        assertFalse(response.body().contains("Current hourly rate: $1/hr"));
    }

    @Test
    void pastedJobPostDraft_keepsSignOnBonusSeparateFromPayRange() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("Los Angeles, CA Registered Nurse (RN) / Full Time - Day Shift 7:00am - 7:30pm. "
                + "Pay Rate $55.67 - $75.63 / hour. $7,500 Sign on Bonus.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Posted pay range: $55.67 - $75.63/hr (using floor $55.67/hr)"));
        assertTrue(response.body().contains("Sign-on bonus: $7,500"));
        assertFalse(response.body().contains("Sign-on bonus: $75.63"));
    }

    @Test
    void pastedJobPostDraft_ignoresImplausiblyLowBonusLikelyCausedByOcrNoise() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("Mobile, AL Registered Nurse hiring event. Sign-On Bonus $6. "
                + "Relocation $5,000 for relocation expenses.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Potential OCR issue: ignored an unusually low sign-on amount."));
        assertTrue(response.body().contains("Relocation support: $5,000"));
        assertFalse(response.body().contains("Sign-on bonus: $6"));
    }

    @Test
    void pastedJobPostDraft_skipsUncontextualizedCityFallbackWhenBrochureListsManyDestinations() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("Infirmary Health RN hiring event in Mobile, AL. "
                + "Proximity to destinations: New Orleans, LA 131 miles. Atlanta, GA 303 miles. "
                + "The hospital includes a freestanding emergency department and other affiliated entities. "
                + "Relocation $5,000 for relocation expenses.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Relocation support: $5,000"));
        assertFalse(response.body().contains("Current city: New Orleans, LA"));
        assertFalse(response.body().contains("Offer city: Atlanta, GA"));
        assertFalse(response.body().contains("Unit: Emergency department"));
    }

    @Test
    void pastedJobPostDraft_normalizesMalformedPdfPayRangeBeforeParsing() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("Department: Adult Medicine & Pediatrics. Salary Range: $35.00 - 49.42.00 /hour. "
                + "Offering: $5000 sign on Bonus.");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Posted pay range: $35 - $49.42/hr (using floor $35/hr)"));
        assertTrue(response.body().contains("Sign-on bonus: $5,000"));
        assertFalse(response.body().contains("Current hourly rate: $42/hr"));
    }

    @Test
    void pastedJobPostDraft_handlesOcrDamagedPhonePhotoText() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("ERED NURSE\n"
                + "-E WA TELEMETRY\n"
                + "NGE MINIMUM $45.59 HOURLY\n"
                + "NGE MAXIMUM $84.47 HOURLY\n"
                + "BONUS $7500");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Role: Registered Nurse"));
        assertTrue(response.body().contains("Unit: Med-surg / telemetry"));
        assertTrue(response.body().contains("Posted pay range: $45.59 - $84.47/hr (using floor $45.59/hr)"));
        assertTrue(response.body().contains("Sign-on bonus: $7,500"));
    }

    @Test
    void pastedJobPostDraft_handlesOcrDamagedSeparatedRangeAndPremiumLines() throws Exception {
        String body = "variant=/nurse-relocation-offer-checker&analysisMode=job_post&sourceText="
                + encode("Harborview Medical Center 3EH Medicine Telemetry RN\n"
                + "90% FTE 12hr shifts 7pm - 7:30am Night Shin\n"
                + "Seattio, WA First Hill\n"
                + "Pay Range Minknum $46.99 hourly\n"
                + "Pay Range Maximum $84.47 hourly\n"
                + "Night Shit Premium $6.80/hour\n"
                + "Weekend Shit Premium $4.00/hour");
        HttpResponse<String> response = httpPostForm("/offer-risk-draft", body);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Unit: Med-surg / telemetry"));
        assertTrue(response.body().contains("Scheduled hours: 36/week"));
        assertTrue(response.body().contains("Posted pay range: $46.99 - $84.47/hr (using floor $46.99/hr)"));
        assertTrue(response.body().contains("Night differential: 14.47%"));
        assertTrue(response.body().contains("Weekend differential: 8.51%"));
        assertFalse(response.body().contains("Offer hourly rate: $4/hr"));
    }

    @Test
    void legacyDecisionLandings_redirectToV2Tool() throws Exception {
        HttpResponse<String> response = httpGetWithoutRedirect("/should-i-take-this-offer");

        assertEquals(301, response.statusCode());
        assertEquals("/nurse-relocation-offer-checker", response.headers().firstValue("location").orElse(null));
    }

    @Test
    void legacyStartPage_redirectsToV2ToolWhenEmpty() throws Exception {
        HttpResponse<String> response = httpGetWithoutRedirect("/start");

        assertEquals(301, response.statusCode());
        assertEquals("/nurse-relocation-offer-checker", response.headers().firstValue("location").orElse(null));
    }

    @Test
    void legacyComparisonPages_redirectToV2ToolAfterPivot() throws Exception {
        HttpResponse<String> response = httpGetWithoutRedirect("/software-engineer-salary-austin-tx-vs-dallas-tx");

        assertEquals(301, response.statusCode());
        assertEquals("/nurse-relocation-offer-checker", response.headers().firstValue("location").orElse(null));
    }

    @Test
    void legacySingleCityPages_redirectToV2ToolAfterPivot() throws Exception {
        HttpResponse<String> response = httpGetWithoutRedirect("/salary-check/software-engineer/austin-tx/100000");

        assertEquals(301, response.statusCode());
        assertEquals("/nurse-relocation-offer-checker", response.headers().firstValue("location").orElse(null));
    }

    @Test
    void legacyHubPages_redirectToV2ToolAfterPivot() throws Exception {
        HttpResponse<String> jobHub = httpGetWithoutRedirect("/job/software-engineer");
        HttpResponse<String> cityHub = httpGetWithoutRedirect("/city/austin-tx");

        assertEquals(301, jobHub.statusCode());
        assertEquals(301, cityHub.statusCode());
        assertEquals("/nurse-relocation-offer-checker", jobHub.headers().firstValue("location").orElse(null));
        assertEquals("/nurse-relocation-offer-checker", cityHub.headers().firstValue("location").orElse(null));
    }

    @Test
    void sitemap_onlyIncludesV2ToolSurface() throws Exception {
        HttpResponse<String> response = httpGet("/sitemap.xml");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/nurse-relocation-offer-checker</loc>"));
        assertTrue(response.body().contains("/sign-on-bonus-repayment-calculator</loc>"));
        assertTrue(response.body().contains("/shift-differential-calculator</loc>"));
        assertTrue(response.body().contains("/should-i-sign-nurse-job-offer</loc>"));
        assertTrue(response.body().contains("/nurse-offer-life-fit</loc>"));
        assertTrue(response.body().contains("/nurse-offer-family-relocation</loc>"));
        assertTrue(response.body().contains("/rn-sign-on-bonus-clawback</loc>"));
        assertTrue(response.body().contains("/rn-float-policy-offer</loc>"));
        assertTrue(response.body().contains("/rn-low-census-cancellation</loc>"));
        assertTrue(response.body().contains("/icu-nurse-offer-red-flags</loc>"));
        assertTrue(response.body().contains("/ed-nurse-offer-red-flags</loc>"));
        assertTrue(response.body().contains("/labor-delivery-nurse-offer-red-flags</loc>"));
        assertFalse(response.body().contains("/salary-check/"));
        assertFalse(response.body().contains("/job/"));
        assertFalse(response.body().contains("/city/"));
        assertFalse(response.body().contains("/should-i-take-this-offer"));
    }

    private HttpResponse<String> httpGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> httpGetWithoutRedirect(String path) throws Exception {
        HttpClient nonRedirectingClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return nonRedirectingClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> httpPostForm(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
