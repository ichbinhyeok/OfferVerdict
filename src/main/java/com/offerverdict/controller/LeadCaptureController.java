package com.offerverdict.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadCaptureController {

    private static final Logger logger = LoggerFactory.getLogger(LeadCaptureController.class);
    private static final String CSV_FILE_PATH = "leads.csv";

    @PostMapping("/capture")
    public ResponseEntity<Map<String, String>> captureLead(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String intent = payload.get("intent");
        String citySlug = payload.get("citySlug");
        String jobSlug = payload.getOrDefault("jobSlug", "none");

        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid email"));
        }

        // Clean inputs to avoid CSV injection
        email = email.replace(",", " ").replace("\"", " ");
        intent = intent != null ? intent.replace(",", " ") : "unknown";
        citySlug = citySlug != null ? citySlug.replace(",", " ") : "unknown";
        jobSlug = jobSlug.replace(",", " ");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String csvLine = String.format("%s,%s,%s,%s,%s", timestamp, email, intent, citySlug, jobSlug);

        java.io.File csvFile = new java.io.File(CSV_FILE_PATH);
        boolean isNewFile = !csvFile.exists();

        try (FileWriter fw = new FileWriter(csvFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {

            if (isNewFile) {
                out.println("timestamp,email,intent,citySlug,jobSlug");
            }
            out.println(csvLine);
            logger.info("New lead captured: {}", csvLine);

        } catch (IOException e) {
            logger.error("Failed to write lead to CSV", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Server error"));
        }

        return ResponseEntity.ok(Map.of("status", "success", "message", "Lead captured"));
    }
}
