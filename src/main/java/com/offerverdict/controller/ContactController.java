package com.offerverdict.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class ContactController {

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About OfferVerdict | Healthcare Offer Risk Tool");
        model.addAttribute("metaDescription",
                "OfferVerdict helps healthcare workers review relocation offers, sign-on bonus repayment, shift differentials, and real monthly cash before accepting.");
        return "about";
    }

    @GetMapping("/contact")
    public String contact(
            @RequestParam(name = "intent", required = false) String intent,
            @RequestParam(name = "analysisMode", required = false) String analysisMode,
            @RequestParam(name = "verdict", required = false) String verdict,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "city", required = false) String city,
            Model model) {
        model.addAttribute("title", "Contact OfferVerdict");
        model.addAttribute("metaDescription", "Contact OfferVerdict about healthcare offer risk, relocation, and bonus repayment feedback.");
        IntentPreset preset = intentPreset(intent, analysisMode, verdict, role, city);
        model.addAttribute("intentTitle", preset.title());
        model.addAttribute("intentLead", preset.lead());
        model.addAttribute("intentLabel", preset.label());
        model.addAttribute("contextMode", friendlyModeLabel(analysisMode));
        model.addAttribute("contextVerdict", cleanOrFallback(verdict, "Not provided"));
        model.addAttribute("contextRole", cleanOrFallback(role, "Healthcare role"));
        model.addAttribute("contextCity", cleanOrFallback(city, "Offer city"));
        model.addAttribute("mailtoHref", preset.mailtoHref());
        return "contact";
    }

    private IntentPreset intentPreset(String intent, String analysisMode, String verdict, String role, String city) {
        String safeIntent = intent == null ? "" : intent.trim().toLowerCase();
        String modeLabel = friendlyModeLabel(analysisMode);
        String verdictLabel = cleanOrFallback(verdict, "No saved verdict");
        String roleLabel = cleanOrFallback(role, "Healthcare role");
        String cityLabel = cleanOrFallback(city, "Offer city");

        String title = "Send healthcare offer feedback.";
        String lead = "If the tool misses a real-world offer term, repayment clause, shift rule, or relocation edge case, send it in.";
        String label = "General feedback";

        if ("second_review".equals(safeIntent)) {
            title = "I would want a second set of eyes on this offer.";
            lead = "This is the strongest post-report signal. It means the free read was useful, but you would still want human review before deciding.";
            label = "Second review";
        } else if ("better_offers".equals(safeIntent)) {
            title = "I would want better offers to compare against this one.";
            lead = "This tells us the next valuable step is not more math on the same package, but access to stronger RN options in the same market.";
            label = "Better offers";
        } else if ("scan_ocr_feedback".equals(safeIntent)) {
            title = "I would want scan OCR to be more reliable on messy offer pages.";
            lead = "This is a friction signal. It means the biggest blocker is screenshots, photos, or scanned PDFs that still need cleanup before the read feels trustworthy.";
            label = "Scan OCR feedback";
        }

        String subject = "OfferVerdict follow-up: " + label;
        String body = """
                Hi,

                I used OfferVerdict and this is the follow-up I would want:
                %s

                Result context:
                - mode: %s
                - verdict: %s
                - role: %s
                - city: %s

                My note:
                """.formatted(label, modeLabel, verdictLabel, roleLabel, cityLabel);

        String mailtoHref = "mailto:contact@livingcostcheck.com?subject="
                + urlEncode(subject)
                + "&body="
                + urlEncode(body);
        return new IntentPreset(title, lead, label, mailtoHref);
    }

    private String friendlyModeLabel(String analysisMode) {
        return "job_post".equalsIgnoreCase(cleanOrFallback(analysisMode, "")) ? "Job post screen" : "Offer review";
    }

    private String cleanOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record IntentPreset(String title, String lead, String label, String mailtoHref) {
    }
}
