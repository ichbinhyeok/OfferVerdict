package com.offerverdict.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.LoadState;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(ExecutionMode.SAME_THREAD)
class PlaywrightBetaSmokeTest {

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
    void v2Home_desktopShowsOfferRiskTool() {
        try (Session session = openDesktopSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator("h1:has-text('Paste the RN offer that is making you hesitate.')").isVisible());
            assertTrue(page.locator("button:has-text('Review an offer letter')").isVisible());
            assertTrue(page.locator("button:has-text('Only have a job post?')").isVisible());
            assertTrue(page.locator("#offer-review-panel form[action='/offer-risk-draft']").isVisible());
            assertTrue(page.locator("text=Offer intake").isVisible());
            assertTrue(page.locator("text=No PDF required").isVisible());
            assertTrue(page.locator("#offer-review-panel input[type='file'][name='sourceFile']").isVisible());
            assertTrue(page.locator("text=No clean letter yet? Confirm only the terms the tool could not read.").isVisible());
            assertTrue(page.locator("#offer-review-panel details.manual-entry").isVisible());
            assertTrue(page.locator("text=Keep the wedge narrow. Keep the exposure surface broad.").isVisible());
            assertFalse(page.locator("#searchForm").isVisible());

            screenshot(page, "v2-home-desktop");
        }
    }

    @Test
    @Order(2)
    void v2JobPostMode_switchesPanelsAndShowsQuickScreenFields() {
        try (Session session = openDesktopSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.click("button:has-text('Only have a job post?')");

            assertTrue(page.locator("#job-post-panel").isVisible());
            assertTrue(page.locator("text=Fallback screen").isVisible());
            assertTrue(page.locator("text=Should this listing even get your time?").isVisible());
            assertTrue(page.locator("#job-post-panel input[type='file'][name='sourceFile']").isVisible());
            assertTrue(page.locator("#job-post-panel input[name='offerHourlyRate']").isVisible());
            assertTrue(page.locator("#job-post-panel select[name='shiftGuarantee']").isVisible());
            assertFalse(page.locator("#offer-review-panel").isVisible());

            screenshot(page, "v2-job-post-mode");
        }
    }

    @Test
    @Order(3)
    void v2PasteInFlow_prefillsFieldsFromOfferText() {
        try (Session session = openDesktopSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.locator("#offer-review-panel form[action='/offer-risk-draft'] textarea[name='sourceText']").fill(
                    "Current job: RN in Austin, TX at $42/hr. New ICU RN offer in Seattle, WA at $60/hr. "
                            + "Sign-on bonus $15000. Relocation stipend $4000. 24 month commitment. "
                            + "Night shift 7p-7a. Hospital-wide float. Can cancel without pay.");
            page.click("#offer-review-panel button:has-text('Build from my input')");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator("text=Auto-filled").first().isVisible());
            assertTrue(page.locator("text=Check what we understood before the verdict.").isVisible());
            assertTrue(page.locator("text=Needs a few facts").isVisible());
            assertEquals("seattle-wa", page.locator("#offer-review-panel select[name='offerCitySlug']").inputValue());
            assertEquals("icu", page.locator("#offer-review-panel select[name='unitType']").inputValue());
            assertTrue(page.locator("#offer-review-panel input[name='offerHourlyRate']").inputValue().startsWith("60"));
            assertTrue(page.locator("#offer-review-panel input[name='signOnBonus']").inputValue().startsWith("15000"));

            screenshot(page, "v2-paste-draft");
        }
    }

    @Test
    @Order(4)
    void v2PdfUploadFlow_prefillsFieldsFromUploadedOffer() throws IOException {
        try (Session session = openDesktopSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.locator("#offer-review-panel input[name='sourceFile']")
                    .setInputFiles(new FilePayload[] {
                            pdfPayload("offer-letter.pdf",
                                    "Current job: RN in Austin, TX at $42/hr.",
                                    "New ICU RN offer in Seattle, WA at $60/hr.",
                                    "Sign-on bonus $15000.",
                                    "Relocation stipend $4000.",
                                    "24 month commitment.",
                                    "Night shift 7p-7a.",
                                    "Hospital-wide float.",
                                    "Can cancel without pay.")
                    });
            page.click("#offer-review-panel button:has-text('Build from my input')");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator("text=Uploaded PDF: offer-letter.pdf").isVisible());
            assertTrue(page.locator("text=Input evidence we used").isVisible());
            assertEquals("seattle-wa", page.locator("#offer-review-panel select[name='offerCitySlug']").inputValue());
            assertEquals("icu", page.locator("#offer-review-panel select[name='unitType']").inputValue());
            assertTrue(page.locator("#offer-review-panel input[name='offerHourlyRate']").inputValue().startsWith("60"));

            screenshot(page, "v2-upload-pdf");
        }
    }

    @Test
    @Order(5)
    void v2ImageUploadFlow_prefillsFieldsFromOfferScreenshot() throws IOException {
        try (Session session = openDesktopSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.locator("#offer-review-panel input[name='sourceFile']")
                    .setInputFiles(new FilePayload[] {
                            pngPayload("offer-shot.png",
                                    "CURRENT RN AUSTIN TX $42/HR",
                                    "NEW ICU RN OFFER SEATTLE WA $60/HR",
                                    "SIGN ON BONUS $15000",
                                    "RELOCATION STIPEND $4000",
                                    "24 MONTH COMMITMENT",
                                    "HOSPITAL WIDE FLOAT",
                                    "CAN CANCEL WITHOUT PAY")
                    });
            page.setDefaultTimeout(120000);
            page.click("#offer-review-panel button:has-text('Build from my input')");
            page.waitForURL("**/offer-risk-draft**");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.setDefaultTimeout(20000);

            assertTrue(page.locator("text=Uploaded image OCR: offer-shot.png").isVisible());
            assertEquals("seattle-wa", page.locator("#offer-review-panel select[name='offerCitySlug']").inputValue());
            assertEquals("icu", page.locator("#offer-review-panel select[name='unitType']").inputValue());
            assertTrue(page.locator("#offer-review-panel input[name='offerHourlyRate']").inputValue().startsWith("60"));
            assertTrue(page.locator("#offer-review-panel input[name='signOnBonus']").inputValue().startsWith("15000"));

            screenshot(page, "v2-upload-image");
        }
    }

    @Test
    @Order(6)
    void v2ReportFlow_carriesDocumentEvidenceIntoDecision() {
        try (Session session = openDesktopSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.locator("#offer-review-panel form[action='/offer-risk-draft'] textarea[name='sourceText']").fill(
                    "Current job: RN in Austin, TX at $42/hr. New ICU RN offer in Seattle, WA at $60/hr. "
                            + "Sign-on bonus $15000. Relocation stipend $4000. 24 month commitment with prorated repayment. "
                            + "Night shift 7p-7a. Hospital-wide float. Can cancel without pay.");
            page.click("#offer-review-panel button:has-text('Build from my input')");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.click("#offer-review-panel details.manual-entry button:has-text('Review this offer')");
            page.waitForURL("**/offer-risk-report**");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator(".report-hero .eyebrow").isVisible());
            assertTrue(page.locator("text=Biggest risks in this packet").isVisible());
            assertTrue(page.locator("text=Can you survive this ICU / critical care offer?").isVisible());
            assertTrue(page.locator("text=Why this verdict").isVisible());
            assertTrue(page.locator("text=What could still change this").isVisible());
            assertTrue(page.locator("text=Do not sign until").first().isVisible());
            assertTrue(page.locator("text=Walk-away line").isVisible());
            assertTrue(page.locator("text=Evidence from your input").isVisible());
            assertTrue(page.locator("text=Pasted offer text").isVisible());
            assertTrue(page.locator("text=A PDF is optional").isVisible());
            assertTrue(page.locator("text=Copy this negotiation note").first().isVisible());
            assertTrue(page.locator("text=What would help next?").isVisible());
            assertTrue(page.locator("text=I would want a second review").isVisible());
            assertTrue(page.locator("text=Repayment exposure").first().isVisible());
            assertTrue(page.locator("text=Nurse schedule risk").isVisible());
            assertTrue(page.locator("text=Written terms on paper").isVisible());
            assertTrue(page.locator("text=Ask these before you sign").isVisible());
            assertTrue(page.locator("text=How to salvage the offer").isVisible());

            screenshot(page, "v2-report-flow");
        }
    }

    @Test
    @Order(7)
    void v2ReportIntentSignal_opensPrefilledContactFlow() {
        try (Session session = openDesktopSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/offer-risk-report");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.click("text=Tell me when scan OCR gets stronger");
            page.waitForURL("**/contact**");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator("text=Scan OCR feedback").isVisible());
            assertTrue(page.locator("text=Open prefilled email").isVisible());
            assertTrue(page.locator("text=This is not a paywall").isVisible());

            screenshot(page, "v2-contact-intent");
        }
    }

    @Test
    @Order(8)
    void v2Home_mobileHasNoHorizontalOverflow() {
        try (Session session = openMobileSession()) {
            Page page = session.page;
            page.navigate(baseUrl() + "/");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertTrue(page.locator("h1:has-text('Paste the RN offer that is making you hesitate.')").isVisible());
            assertTrue(page.locator("button:has-text('Only have a job post?')").isVisible());
            boolean noHorizontalOverflow = (Boolean) page.evaluate(
                    "() => document.documentElement.scrollWidth <= (window.innerWidth + 2)");
            assertTrue(noHorizontalOverflow, "V2 mobile home should not overflow horizontally");

            screenshot(page, "v2-home-mobile");
        }
    }

    @Test
    @Order(9)
    void v2ContentPages_matchPivotMessaging() {
        try (Session session = openDesktopSession()) {
            Page page = session.page;

            page.navigate(baseUrl() + "/about");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            assertTrue(page.locator("h1:has-text('healthcare offer-risk tool')").isVisible());
            screenshot(page, "v2-about");

            page.navigate(baseUrl() + "/methodology");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            assertTrue(page.locator("h1:has-text('offer risk report')").isVisible());
            assertTrue(page.locator("text=Bonus and relocation money are treated as refundable risk").isVisible());
            screenshot(page, "v2-methodology");
        }
    }

    @Test
    @Order(10)
    void retiredLegacyPagesResolveToV2Tool() throws Exception {
        assertRedirectedToTool("/start");
        assertRedirectedToTool("/cities");
        assertRedirectedToTool("/job/software-engineer");
        assertRedirectedToTool("/city/austin-tx");
        assertRedirectedToTool("/salary-check/software-engineer/austin-tx/100000");
        assertRedirectedToTool("/software-engineer-salary-austin-tx-vs-dallas-tx");
    }

    @Test
    @Order(11)
    void v2SeoSurface_hasNarrowSitemapAndNoindexReport() throws Exception {
        HttpResponse<String> home = httpGet("/");
        HttpResponse<String> report = httpGet("/offer-risk-report");
        HttpResponse<String> sitemap = httpGet("/sitemap.xml");

        assertEquals(200, home.statusCode());
        assertEquals(200, report.statusCode());
        assertEquals(200, sitemap.statusCode());
        assertTrue(home.body().contains("name=\"robots\" content=\"index, follow\""));
        assertTrue(report.body().contains("name=\"robots\" content=\"noindex, follow\""));
        assertTrue(sitemap.body().contains("/nurse-relocation-offer-checker</loc>"));
        assertFalse(sitemap.body().contains("/salary-check/"));
        assertFalse(sitemap.body().contains("/job/"));
        assertFalse(sitemap.body().contains("/city/"));
    }

    private Session openDesktopSession() {
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1440, 900)
                .setLocale("en-US"));
        Page page = context.newPage();
        page.setDefaultTimeout(20000);
        return new Session(context, page);
    }

    private Session openMobileSession() {
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
        return new Session(context, page);
    }

    private void assertRedirectedToTool(String path) throws IOException, InterruptedException {
        HttpResponse<String> response = httpGet(path);
        assertEquals(200, response.statusCode());
        assertTrue(response.uri().toString().endsWith("/nurse-relocation-offer-checker"),
                "Expected " + path + " to resolve to V2 tool, got " + response.uri());
    }

    private void screenshot(Page page, String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(artifactsDir.resolve(name + ".png"))
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

    private FilePayload pdfPayload(String filename, String... lines) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 12);
                content.newLineAtOffset(50, 720);
                for (String line : lines) {
                    content.showText(line);
                    content.newLineAtOffset(0, -18);
                }
                content.endText();
            }

            document.save(output);
            return new FilePayload(filename, "application/pdf", output.toByteArray());
        }
    }

    private FilePayload pngPayload(String filename, String... lines) throws IOException {
        int width = 1900;
        int height = Math.max(1100, 120 + (lines.length * 110));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 44));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int y = 110;
            for (String line : lines) {
                graphics.drawString(line, 80, y);
                y += 100;
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return new FilePayload(filename, "image/png", output.toByteArray());
        }
    }

    private static final class Session implements AutoCloseable {
        private final BrowserContext context;
        private final Page page;

        private Session(BrowserContext context, Page page) {
            this.context = context;
            this.page = page;
        }

        @Override
        public void close() {
            context.close();
        }
    }
}
