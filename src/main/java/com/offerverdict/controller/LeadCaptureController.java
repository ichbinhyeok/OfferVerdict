package com.offerverdict.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/leads")
public class LeadCaptureController {

    private static final Logger logger = LoggerFactory.getLogger(LeadCaptureController.class);
    private static final Pattern BASIC_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String LEAD_CSV_NAME = "leads.csv";
    private static final String LEAD_EVENT_CSV_NAME = "lead_events.csv";
    private static final String LEAD_CSV_HEADER = "timestamp,email,intent,citySlug,jobSlug,sourcePath,referrer,userAgent,ip";
    private static final String LEAD_EVENT_CSV_HEADER = "timestamp,eventName,intent,citySlug,jobSlug,sourcePath,referrer,userAgent,ip";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Value("${app.leads.storageDir:./data/leads}")
    private String leadsStorageDir;

    @Value("${app.leads.backupDir:./data/leads-backup}")
    private String leadsBackupDir;

    @Value("${app.leads.dedupeMinutes:15}")
    private int dedupeMinutes;

    private final Object csvWriteLock = new Object();
    private final Map<String, LocalDateTime> recentLeadKeys = new ConcurrentHashMap<>();

    @PostMapping("/capture")
    public ResponseEntity<Map<String, String>> captureLead(@RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        String email = sanitize(payload.get("email"), 320).toLowerCase();
        String intent = sanitize(payload.get("intent"), 80);
        String citySlug = sanitize(payload.get("citySlug"), 80);
        String jobSlug = sanitize(payload.getOrDefault("jobSlug", "none"), 80);
        String sourcePath = sanitize(payload.get("sourcePath"), 400);
        String referrer = sanitize(payload.get("referrer"), 400);
        String userAgent = sanitize(request.getHeader("User-Agent"), 400);
        String ip = sanitize(resolveClientIp(request), 120);

        String honeypot = payload.get("honeypot");

        if (honeypot != null && !honeypot.trim().isEmpty()) {
            logger.warn("Bot detected via honeypot field. Dropping request.");
            safeWriteLeadEvent("bot_honeypot", intent, citySlug, jobSlug, sourcePath, referrer, userAgent, ip);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Lead captured"));
        }

        if (email.isBlank() || !BASIC_EMAIL_PATTERN.matcher(email).matches()) {
            safeWriteLeadEvent("lead_submit_invalid", intent, citySlug, jobSlug, sourcePath, referrer, userAgent, ip);
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid email"));
        }

        if (intent.isBlank()) {
            intent = "unknown";
        }
        if (citySlug.isBlank()) {
            citySlug = "unknown";
        }
        if (jobSlug.isBlank()) {
            jobSlug = "none";
        }
        String dedupeKey = buildLeadDedupeKey(email, intent, citySlug, jobSlug);

        if (isDuplicateLead(dedupeKey)) {
            safeWriteLeadEvent("lead_submit_duplicate", intent, citySlug, jobSlug, sourcePath, referrer, userAgent, ip);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Lead already captured recently"));
        }

        try {
            appendCsvRow(
                    LEAD_CSV_NAME,
                    LEAD_CSV_HEADER,
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    email,
                    intent,
                    citySlug,
                    jobSlug,
                    sourcePath,
                    referrer,
                    userAgent,
                    ip);

            appendCsvRow(
                    LEAD_EVENT_CSV_NAME,
                    LEAD_EVENT_CSV_HEADER,
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    "lead_submit_success",
                    intent,
                    citySlug,
                    jobSlug,
                    sourcePath,
                    referrer,
                    userAgent,
                    ip);
            rememberLeadKey(dedupeKey);

            logger.info("New lead captured: email={}, intent={}, city={}, job={}", email, intent, citySlug, jobSlug);
        } catch (IOException e) {
            logger.error("Failed to write lead to CSV", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Server error"));
        }

        return ResponseEntity.ok(Map.of("status", "success", "message", "Lead captured"));
    }

    @PostMapping("/event")
    public ResponseEntity<Map<String, String>> trackLeadEvent(@RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        String eventName = sanitize(payload.get("eventName"), 80);
        if (eventName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Missing eventName"));
        }

        String intent = sanitize(payload.get("intent"), 80);
        String citySlug = sanitize(payload.get("citySlug"), 80);
        String jobSlug = sanitize(payload.getOrDefault("jobSlug", "none"), 80);
        String sourcePath = sanitize(payload.get("sourcePath"), 400);
        String referrer = sanitize(payload.get("referrer"), 400);
        String userAgent = sanitize(request.getHeader("User-Agent"), 400);
        String ip = sanitize(resolveClientIp(request), 120);

        try {
            appendCsvRow(
                    LEAD_EVENT_CSV_NAME,
                    LEAD_EVENT_CSV_HEADER,
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    eventName,
                    intent,
                    citySlug,
                    jobSlug,
                    sourcePath,
                    referrer,
                    userAgent,
                    ip);
        } catch (IOException e) {
            logger.error("Failed to write lead event to CSV", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", "Server error"));
        }

        return ResponseEntity.ok(Map.of("status", "success", "message", "Event recorded"));
    }

    private void safeWriteLeadEvent(String eventName, String intent, String citySlug, String jobSlug,
            String sourcePath, String referrer, String userAgent, String ip) {
        try {
            appendCsvRow(
                    LEAD_EVENT_CSV_NAME,
                    LEAD_EVENT_CSV_HEADER,
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    sanitize(eventName, 80),
                    sanitize(intent, 80),
                    sanitize(citySlug, 80),
                    sanitize(jobSlug, 80),
                    sanitize(sourcePath, 400),
                    sanitize(referrer, 400),
                    sanitize(userAgent, 400),
                    sanitize(ip, 120));
        } catch (IOException e) {
            logger.warn("Failed to write fallback lead event {}", eventName, e);
        }
    }

    private void appendCsvRow(String fileName, String header, String... columns) throws IOException {
        Path dirPath = Paths.get(leadsStorageDir).toAbsolutePath().normalize();
        Path backupDirPath = Paths.get(leadsBackupDir).toAbsolutePath().normalize();
        LocalDate today = LocalDate.now();
        String dailyName = withDateSuffix(fileName, today);
        String csvRow = toCsv(columns);

        synchronized (csvWriteLock) {
            Files.createDirectories(dirPath);
            Files.createDirectories(backupDirPath);

            writeCsvLine(dirPath.resolve(fileName), header, csvRow);
            writeCsvLine(dirPath.resolve(dailyName), header, csvRow);
            writeCsvLine(backupDirPath.resolve(dailyName), header, csvRow);
        }
    }

    private void writeCsvLine(Path filePath, String header, String csvRow) throws IOException {
        boolean shouldWriteHeader = !Files.exists(filePath) || Files.size(filePath) == 0;
        StringBuilder payload = new StringBuilder(256);
        if (shouldWriteHeader) {
            payload.append(header).append(System.lineSeparator());
        }
        payload.append(csvRow).append(System.lineSeparator());

        try (FileChannel channel = FileChannel.open(
                filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {
            channel.write(ByteBuffer.wrap(payload.toString().getBytes(StandardCharsets.UTF_8)));
            channel.force(true);
        }
    }

    private String withDateSuffix(String fileName, LocalDate date) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName + "-" + date;
        }
        return fileName.substring(0, dotIndex) + "-" + date + fileName.substring(dotIndex);
    }

    private String toCsv(String... values) {
        return Arrays.stream(values).map(this::escapeCsv).reduce((a, b) -> a + "," + b).orElse("");
    }

    private String escapeCsv(String value) {
        String normalized = sanitize(value, 1000);
        boolean needsQuotes = normalized.contains(",") || normalized.contains("\"");
        if (!needsQuotes) {
            return normalized;
        }
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private String sanitize(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace("\r", " ").replace("\n", " ").trim();
        if (cleaned.length() > maxLen) {
            return cleaned.substring(0, maxLen);
        }
        return cleaned;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildLeadDedupeKey(String email, String intent, String citySlug, String jobSlug) {
        String payload = String.join("|", sanitize(email, 320), sanitize(intent, 80), sanitize(citySlug, 80),
                sanitize(jobSlug, 80));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return payload;
        }
    }

    private boolean isDuplicateLead(String dedupeKey) {
        int windowMinutes = Math.max(1, dedupeMinutes);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime previous = recentLeadKeys.get(dedupeKey);
        pruneDedupeCache(now, windowMinutes);
        if (previous == null) {
            return false;
        }
        return Duration.between(previous, now).toMinutes() < windowMinutes;
    }

    private void rememberLeadKey(String dedupeKey) {
        recentLeadKeys.put(dedupeKey, LocalDateTime.now());
    }

    private void pruneDedupeCache(LocalDateTime now, int windowMinutes) {
        if (recentLeadKeys.size() <= 10_000) {
            return;
        }
        LocalDateTime threshold = now.minusMinutes(windowMinutes * 2L);
        recentLeadKeys.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}
