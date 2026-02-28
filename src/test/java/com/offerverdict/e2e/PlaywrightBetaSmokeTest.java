package com.offerverdict.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(ExecutionMode.SAME_THREAD)
class PlaywrightBetaSmokeTest {

    private static final Pattern CANONICAL_PATTERN = Pattern.compile("<link\\s+rel=\"canonical\"\\s+href=\"([^\"]+)\"");

    @LocalServerPort
    int port;

    private Playwright playwright;
    private Browser browser;
    private HttpClient httpClient;
    private Path artifactsDir;

    @BeforeAll
    void setupPlaywright() throws IOException {
        artifactsDir = Paths.get("build", "reports", "playwright-beta");
        Files.createDirectories(artifactsDir);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @AfterAll
    void teardownPlaywright() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @Order(1)
    void persona01_homeSmoke_singleModeDefaults() {
        try (Session session = openDesktopSession("persona01-home-smoke")) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator("#searchForm").isVisible());
            assertTrue(page.locator("#jobInput").isVisible());
            assertTrue(page.locator("#cityInputA").isVisible());
            assertTrue(page.locator("input[name='currentSalary']").isVisible());
            assertTrue(page.locator("#cityInputB").isDisabled());
            assertEquals("Check My Salary", page.locator("button[type='submit']").innerText().trim());

            screenshot(page, "persona01-home-smoke");
        }
    }

    @Test
    @Order(2)
    void persona02_singleSalaryJourney_midCareer() {
        try (Session session = openDesktopSession("persona02-single-flow")) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");

            page.fill("#jobInput", "Software Engineer");
            page.fill("#cityInputA", "Austin, TX");
            page.fill("input[name='currentSalary']", "100000");

            page.click("button[type='submit']");
            page.waitForURL("**/salary-check/**");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.url().contains("/salary-check/"));
            assertTrue(page.locator(".verdict-section").first().isVisible());
            assertTrue(page.locator("h3:has-text('Quick Summary')").isVisible());
            assertTrue(page.locator("section.faq-section").first().isVisible());

            screenshot(page, "persona02-single-flow");
        }
    }

    @Test
    @Order(3)
    void persona03_compareOfferJourney_relocationDecision() {
        try (Session session = openDesktopSession("persona03-compare-flow")) {
            Page page = session.page;
            page.navigate(baseUrl() + "/?mode=compare");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.click("#tabCompare");
            page.fill("#jobInput", "Financial Analyst");
            page.fill("#cityInputA", "New York, NY");
            page.fill("input[name='currentSalary']", "120000");
            page.fill("#cityInputB", "Chicago, IL");
            page.fill("input[name='offerSalary']", "145000");

            page.click("button[type='submit']");
            page.waitForURL("**/*-salary-*-vs-*");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.url().contains("-salary-"));
            assertTrue(page.locator(".verdict-section").first().isVisible());
            assertTrue(page.locator("section.faq-section").first().isVisible());

            screenshot(page, "persona03-compare-flow");
        }
    }

    @Test
    @Order(4)
    void persona04_seoSinglePage_noindexOutlierAndCanonical() throws Exception {
        HttpResponse<String> lowSalary = httpGet("/salary-check/software-engineer/austin-tx/20000");
        assertEquals(200, lowSalary.statusCode());
        assertTrue(lowSalary.body().contains("name=\"robots\" content=\"noindex,follow\""));

        Matcher matcher = CANONICAL_PATTERN.matcher(lowSalary.body());
        assertTrue(matcher.find(), "Expected canonical link on single page");
        String canonical = matcher.group(1);
        assertFalse(canonical.contains("?"));

        HttpResponse<String> inRange = httpGet("/salary-check/software-engineer/austin-tx/100000");
        assertEquals(200, inRange.statusCode());
        assertFalse(inRange.body().contains("name=\"robots\" content=\"noindex,follow\""),
                "In-range page should be indexable");
    }

    @Test
    @Order(5)
    void persona05_hubIndexPolicy_sitemapConsistency() throws Exception {
        HttpResponse<String> citiesPage = httpGet("/cities");
        HttpResponse<String> jobHubPage = httpGet("/job/software-engineer");
        HttpResponse<String> sitemap = httpGet("/sitemap.xml");

        assertEquals(200, citiesPage.statusCode());
        assertEquals(200, jobHubPage.statusCode());
        assertEquals(200, sitemap.statusCode());

        assertTrue(citiesPage.body().contains("name=\"robots\" content=\"noindex,follow\""));
        assertTrue(jobHubPage.body().contains("name=\"robots\" content=\"noindex,follow\""));

        assertFalse(sitemap.body().contains("/cities</loc>"));
        assertFalse(sitemap.body().contains("/job/"));
        assertTrue(sitemap.body().contains("/salary-check/"));
    }

    @Test
    @Order(6)
    void persona06_robotsAndCanonicalQueryControl() throws Exception {
        HttpResponse<String> robots = httpGet("/robots.txt");
        assertEquals(200, robots.statusCode());
        assertTrue(robots.body().contains("Disallow: /*?*"));

        try (Session session = openDesktopSession("persona06-canonical-query")) {
            Page page = session.page;
            page.navigate(baseUrl()
                    + "/software-engineer-salary-san-francisco-ca-vs-austin-tx?currentSalary=150000&offerSalary=180000&salaryType=annual");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            String canonical = page.locator("link[rel='canonical']").getAttribute("href");
            assertNotNull(canonical);
            assertFalse(canonical.contains("?"));

            screenshot(page, "persona06-canonical-query");
        }
    }

    @Test
    @Order(7)
    void persona07_leadFunnelAndDuplicateSuppression() throws Exception {
        try (Session session = openDesktopSession("persona07-lead-funnel")) {
            Page page = session.page;
            page.navigate(baseUrl() + "/salary-check/software-engineer/austin-tx/120000");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            AtomicInteger dialogCount = new AtomicInteger(0);
            page.onDialog(dialog -> {
                dialogCount.incrementAndGet();
                dialog.accept();
            });

            page.waitForResponse(resp -> resp.url().contains("/api/leads/event"), () -> {
                page.click("button:has-text('Get Free Review Quote')");
            });

            assertTrue(page.locator("#lead-email").isVisible());
            assertTrue(page.locator("#lead-btn").isVisible());

            page.fill("#lead-email", "invalid-email");
            page.click("#lead-btn");
            assertTrue(dialogCount.get() >= 1, "Expected invalid-email validation dialog");

            String uniqueEmail = "qa+" + System.currentTimeMillis() + "@example.com";
            Response captureResponse = page.waitForResponse(
                    resp -> resp.url().contains("/api/leads/capture") && "POST".equalsIgnoreCase(resp.request().method()),
                    () -> {
                        page.fill("#lead-email", uniqueEmail);
                        page.click("#lead-btn");
                    });

            assertEquals(200, captureResponse.status());
            String captureJson = Optional.ofNullable(captureResponse.text()).orElse("");
            assertTrue(captureJson.toLowerCase(Locale.US).contains("success"));
            assertTrue(page.locator("#lead-msg").isVisible());

            String duplicatePayload = String.format(Locale.US,
                    "{\"email\":\"%s\",\"intent\":\"resume_review\",\"citySlug\":\"austin-tx\",\"jobSlug\":\"software-engineer\",\"sourcePath\":\"/salary-check/software-engineer/austin-tx/120000\",\"referrer\":\"\",\"honeypot\":\"\"}",
                    uniqueEmail);
            HttpResponse<String> duplicate = httpPostJson("/api/leads/capture", duplicatePayload);
            assertEquals(200, duplicate.statusCode());
            assertTrue(duplicate.body().toLowerCase(Locale.US).contains("already captured recently"));

            screenshot(page, "persona07-lead-funnel");
        }
    }

    @Test
    @Order(8)
    void persona08_simulationLabRealtimeResidualUpdate() {
        try (Session session = openDesktopSession("persona08-simulation")) {
            Page page = session.page;
            page.navigate(baseUrl() + "/salary-check/software-engineer/austin-tx/120000");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            String before = page.locator("#disp-residual-top").innerText().trim();
            page.click("#btn-toggle-sim");
            assertTrue(page.locator("#simulation-panel").isVisible());

            page.waitForResponse(resp -> resp.url().contains("/api/simulate-single") && resp.status() == 200, () -> {
                page.locator("#input-sidehustle").evaluate(
                        "el => { el.value = '2000'; el.dispatchEvent(new Event('input', { bubbles: true })); }");
            });

            page.waitForTimeout(900);
            String after = page.locator("#disp-residual-top").innerText().trim();
            assertNotEquals(before, after, "Residual summary should update after simulation inputs");

            screenshot(page, "persona08-simulation");
        }
    }

    @Test
    @Order(9)
    void persona09_mobileSmoke_singleVerdictExperience() {
        try (Session session = openMobileSession("persona09-mobile-smoke")) {
            Page page = session.page;
            page.navigate(baseUrl() + "/salary-check/software-engineer/new-york-ny/150000");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator(".verdict-section").first().isVisible());
            boolean noHorizontalOverflow = (Boolean) page.evaluate(
                    "() => document.documentElement.scrollWidth <= (window.innerWidth + 20)");
            assertTrue(noHorizontalOverflow, "Mobile viewport should not have severe horizontal overflow");

            screenshot(page, "persona09-mobile-smoke");
        }
    }

    @Test
    @Order(10)
    void persona10_structuredDataAndVisibleFaqAlignment() {
        try (Session session = openDesktopSession("persona10-faq-alignment")) {
            Page page = session.page;
            page.navigate(baseUrl()
                    + "/software-engineer-salary-san-francisco-ca-vs-austin-tx?currentSalary=150000&offerSalary=180000");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            String firstVisibleFaqQuestion = page.locator(".faq-grid .faq-item h3").first().innerText().trim();
            String jsonLd = page.locator("script[type='application/ld+json']").first().innerText();

            assertTrue(firstVisibleFaqQuestion.length() > 10);
            assertTrue(jsonLd.contains(firstVisibleFaqQuestion),
                    "Visible FAQ question should also be present in JSON-LD");

            screenshot(page, "persona10-faq-alignment");
        }
    }

    @Test
    @Order(11)
    void persona11_headerFreshness_lastModifiedPresentAndParsable() throws Exception {
        HttpResponse<String> response = httpGet(
                "/software-engineer-salary-san-francisco-ca-vs-austin-tx?currentSalary=150000&offerSalary=180000");
        assertEquals(200, response.statusCode());

        Optional<String> lastModifiedOpt = response.headers().firstValue("last-modified");
        assertTrue(lastModifiedOpt.isPresent(), "Comparison page should emit Last-Modified");

        ZonedDateTime parsed = ZonedDateTime.parse(lastModifiedOpt.get(), DateTimeFormatter.RFC_1123_DATE_TIME);
        assertTrue(parsed.toInstant().isAfter(Instant.parse("2024-01-01T00:00:00Z")));
        assertTrue(parsed.toInstant().isBefore(Instant.now().plusSeconds(86400)));
    }

    @Test
    @Order(12)
    void persona12_multiProfileMatrix_highMediumLowSalaryUseCases() {
        String[][] matrix = new String[][] {
                { "software-engineer", "austin-tx", "90000" },
                { "software-engineer", "new-york-ny", "150000" },
                { "software-engineer", "san-francisco-ca", "250000" }
        };

        for (String[] c : matrix) {
            String label = c[0] + "-" + c[1] + "-" + c[2];
            try (Session session = openDesktopSession("persona12-" + label)) {
                Page page = session.page;
                page.navigate(baseUrl() + "/salary-check/" + c[0] + "/" + c[1] + "/" + c[2]);
                page.waitForLoadState(LoadState.NETWORKIDLE);

                assertTrue(page.locator(".verdict-text").first().isVisible());
                assertTrue(page.locator(".analysis-box").first().isVisible());
                assertTrue(page.locator("section.faq-section").first().isVisible());

                screenshot(page, "persona12-" + label);
            }
        }
    }

    private Session openDesktopSession(String persona) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1440, 900)
                .setLocale("en-US"));
        Page page = context.newPage();
        page.setDefaultTimeout(20000);
        return new Session(persona, context, page);
    }

    private Session openMobileSession(String persona) {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(390, 844)
                .setDeviceScaleFactor(3)
                .setHasTouch(true)
                .setIsMobile(true)
                .setLocale("en-US")
                .setUserAgent(
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"));
        Page page = context.newPage();
        page.setDefaultTimeout(25000);
        return new Session(persona, context, page);
    }

    private void screenshot(Page page, String name) {
        String safeName = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(artifactsDir.resolve(safeName + ".png"))
                .setFullPage(true));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    private HttpResponse<String> httpGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> httpPostJson(String path, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static final class Session implements AutoCloseable {
        private final String persona;
        private final BrowserContext context;
        private final Page page;

        private Session(String persona, BrowserContext context, Page page) {
            this.persona = persona;
            this.context = context;
            this.page = page;
        }

        @Override
        public void close() {
            context.close();
        }
    }
}
