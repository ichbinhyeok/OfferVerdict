package com.offerverdict.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeoRegressionIntegrationTest {

    private static final Pattern JSON_LD_PATTERN = Pattern.compile(
            "<script type=\"application/ld\\+json\">(.*?)</script>",
            Pattern.DOTALL);

    @LocalServerPort
    int port;

    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Test
    void prefilledCompareHome_isNoindexAndCanonicalizedToRoot() throws Exception {
        HttpResponse<String> response = httpGetWithoutRedirect(
                "/?mode=compare&city1=minneapolis-mn&job=architect&salary=100000");

        assertEquals(301, response.statusCode());
        assertEquals("/start#mode=compare&city1=minneapolis-mn&job=architect&salary=100000",
                response.headers().firstValue("location").orElse(null));
    }

    @Test
    void singleCityStructuredData_rendersValidJsonLd() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/architect/minneapolis-mn/100000");

        assertEquals(200, response.statusCode());

        Matcher matcher = JSON_LD_PATTERN.matcher(response.body());
        assertTrue(matcher.find(), "Expected JSON-LD script block");

        String json = matcher.group(1).trim();
        assertFalse(json.contains("[["), "Expected Thymeleaf placeholders to be fully rendered");

        JsonNode root = objectMapper.readTree(json);
        assertNotNull(root.get("@graph"));
        assertTrue(root.get("@graph").isArray());
        assertEquals(2, root.get("@graph").size());
    }

    @Test
    void genericSingleCityPage_isNoindexAfterPruning() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/miami-fl/180000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"robots\" content=\"noindex,follow\""));
    }

    @Test
    void lowPriorityOrNonMajorSingleCityPage_isNoindexAfterPruning() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/architect/minneapolis-mn/100000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"robots\" content=\"noindex,follow\""));
    }

    @Test
    void majorJobAndPriorityCitySingleCityPage_remainsIndexable() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/software-engineer/austin-tx/100000");

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("name=\"robots\" content=\"noindex,follow\""));
    }

    @Test
    void lowValueComparisonPage_isNoindexAfterPruning() throws Exception {
        HttpResponse<String> response = httpGet("/architect-salary-austin-tx-vs-minneapolis-mn");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"robots\" content=\"noindex, follow\""));
    }

    @Test
    void coreComparisonPage_remainsIndexable() throws Exception {
        HttpResponse<String> response = httpGet("/software-engineer-salary-austin-tx-vs-dallas-tx");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"robots\" content=\"index, follow\""));
        assertTrue(response.body().contains("Uses metro-adjusted role pay anchors, plus public tax and cost data"));
    }

    @Test
    void comparisonPage_linksToJobSpecificSinglePagesInsteadOfGenericNoindexPages() throws Exception {
        HttpResponse<String> response = httpGet("/software-engineer-salary-austin-tx-vs-dallas-tx");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/salary-check/software-engineer/austin-tx/"));
        assertTrue(response.body().contains("/salary-check/software-engineer/dallas-tx/"));
        assertFalse(response.body().contains("href=\"/salary-check/austin-tx/"));
        assertFalse(response.body().contains("href=\"/salary-check/dallas-tx/"));
    }

    @Test
    void singleCityPage_linksToCrawlableComparisonAndJobHub() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/software-engineer/austin-tx/100000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/software-engineer-salary-austin-tx-vs-"));
        assertTrue(response.body().contains("/job/software-engineer"));
    }

    @Test
    void majorJobHub_isIndexableAndIncludedAsLandingLayer() throws Exception {
        HttpResponse<String> response = httpGet("/job/software-engineer");

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("name=\"robots\" content=\"noindex,follow\""));
        assertTrue(response.body().contains("/salary-check/software-engineer/"));
    }

    @Test
    void priorityCityHub_isIndexableAndLinksToRoleGuides() throws Exception {
        HttpResponse<String> response = httpGet("/city/austin-tx");

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("name=\"robots\" content=\"noindex,follow\""));
        assertTrue(response.body().contains("/salary-check/registered-nurse/austin-tx/"));
        assertTrue(response.body().contains("/registered-nurse-salary-austin-tx-vs-"));
    }

    @Test
    void lowPriorityCityHub_staysNoindex() throws Exception {
        HttpResponse<String> response = httpGet("/city/minneapolis-mn");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"robots\" content=\"noindex,follow\""));
    }

    @Test
    void home_and_decision_landing_highlight_nonTechRoleGuides() throws Exception {
        HttpResponse<String> home = httpGet("/");
        HttpResponse<String> landing = httpGet("/should-i-take-this-offer");

        assertEquals(200, home.statusCode());
        assertEquals(200, landing.statusCode());
        assertTrue(home.body().contains("/job/registered-nurse"));
        assertTrue(home.body().contains("/job/accountant"));
        assertTrue(home.body().contains("/job/teacher"));
        assertTrue(landing.body().contains("/job/registered-nurse"));
        assertTrue(landing.body().contains("/job/project-manager"));
        assertTrue(landing.body().contains("/job/pharmacist"));
    }

    @Test
    void home_highlights_methodology_and_doesNotRenderLegacySplitHeroCopy() throws Exception {
        HttpResponse<String> response = httpGet("/");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Review the methodology"));
        assertTrue(response.body().contains("Transparent limits"));
        assertFalse(response.body().contains("The left side sets the context. The right side gets you to a verdict fast."));
        assertFalse(response.body().contains("class=\"split-left\""));
    }

    @Test
    void hubPages_labelSalaryAsModelStartingPoint() throws Exception {
        HttpResponse<String> jobHub = httpGet("/job/software-engineer");
        HttpResponse<String> cityHub = httpGet("/city/austin-tx");

        assertEquals(200, jobHub.statusCode());
        assertEquals(200, cityHub.statusCode());
        assertTrue(jobHub.body().contains("public wage median"));
        assertTrue(cityHub.body().contains("public wage median"));
        assertFalse(jobHub.body().contains("Starting salary check:"));
        assertFalse(cityHub.body().contains("Estimated from role baselines, city costs, and local income levels. Review the full assumptions before using it as a negotiation anchor."));
        assertTrue(jobHub.body().contains("/methodology"));
    }

    @Test
    void teacherHub_fallsBackToModeledStartingPointWhenRoleBenchmarkIsMissing() throws Exception {
        HttpResponse<String> response = httpGet("/job/teacher");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Model starting point:"));
        assertTrue(response.body().contains("Estimated from role baselines, city costs, and local income levels."));
    }

    @Test
    void accountantHub_usesRolePayAnchorWhenNationalBenchmarkExists() throws Exception {
        HttpResponse<String> response = httpGet("/job/accountant");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Metro-adjusted role anchor:"));
        assertTrue(response.body().contains("Starts from a public wage median for this role"));
        assertFalse(response.body().contains("Model starting point:"));
    }

    @Test
    void accountantSingleCity_withExactMetroBenchmark_usesLocalMedianAnchorLanguage() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/accountant/austin-tx/80000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void accountantSingleCity_withoutExactMetroBenchmark_usesMetroAdjustedAnchorLanguage() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/accountant/phoenix-az/80000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Metro-adjusted role anchor"));
        assertTrue(response.body().contains("starts from a public role median"));
    }

    @Test
    void seattlePharmacistSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/pharmacist/seattle-wa/140000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void newYorkProjectManagerSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/project-manager/new-york-ny/120000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void sanFranciscoNurseSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/registered-nurse/san-francisco-ca/180000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void austinMarketingManagerSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/marketing-manager/austin-tx/150000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void dallasAccountantSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/accountant/dallas-tx/90000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void bostonProjectManagerSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/project-manager/boston-ma/115000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void losAngelesPhysicalTherapistSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/physical-therapist/los-angeles-ca/110000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void chicagoPharmacistSingleCity_usesLocalAnchorLanguageFromExactMetroData() throws Exception {
        HttpResponse<String> response = httpGet("/salary-check/pharmacist/chicago-il/135000");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Local median pay anchor"));
        assertTrue(response.body().contains("local public wage median"));
    }

    @Test
    void nonMajorJobHub_staysNoindex() throws Exception {
        HttpResponse<String> response = httpGet("/job/architect");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("name=\"robots\" content=\"noindex,follow\""));
    }

    @Test
    void sitemap_includesDecisionLandingAndExcludesGenericSingleCityUrls() throws Exception {
        HttpResponse<String> response = httpGet("/sitemap.xml");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("/should-i-take-this-offer</loc>"));
        assertTrue(response.body().contains("/job/software-engineer</loc>"));
        assertTrue(response.body().contains("/job/registered-nurse</loc>"));
        assertTrue(response.body().contains("/job/accountant</loc>"));
        assertTrue(response.body().contains("/job/teacher</loc>"));
        assertTrue(response.body().contains("/city/austin-tx</loc>"));
        assertTrue(response.body().contains("/city/seattle-wa</loc>"));
        assertTrue(response.body().contains("/city/los-angeles-ca</loc>"));
        assertTrue(response.body().contains("/city/chicago-il</loc>"));
        assertTrue(response.body().contains("/city/boston-ma</loc>"));
        assertFalse(response.body().contains("/salary-check/miami-fl/"));
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
}
