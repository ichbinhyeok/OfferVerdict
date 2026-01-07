package com.offerverdict.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.util.SlugNormalizer;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SalaryDataService {

    private final ObjectMapper objectMapper;
    private Map<String, JobBenchmark> benchmarks = Collections.emptyMap();

    public SalaryDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            JsonNode root = objectMapper
                    .readTree(new ClassPathResource("data/salary-benchmarks-2026.json").getInputStream());
            JsonNode occupations = root.get("occupations");

            // For now, naive loading of 'software-engineer' key logic or generic map
            // We'll flatten it: JobSlug -> CitySlug -> Stats
            // But strict typing suggests mapping "software-engineer" to a JobBenchmark
            // object

            // Simplified: Map job slug to JobBenchmark object
            this.benchmarks = objectMapper.convertValue(occupations, new TypeReference<Map<String, JobBenchmark>>() {
            });

        } catch (IOException e) {
            System.err.println("Warning: Could not load salary-benchmarks-2026.json: " + e.getMessage());
        }
    }

    /**
     * Calculate percentile of the given salary for a specific job in a city
     * Returns a string like "22" (meaning Top 22%) or "45" (Top 45%)
     * Actually, "Top 22%" means you are in the 78th percentile.
     * Use "Top X%" format as requested by UX.
     */
    public String getPercentileLabel(String jobSlug, String citySlug, double salary) {
        // Naive fallback matching for 'software-engineer' if exact match fails,
        // to ensure the demo works for generic "Software Engineer" inputs
        String normalizedJob = SlugNormalizer.normalize(jobSlug);
        if (normalizedJob.contains("software") || normalizedJob.contains("engineer")
                || normalizedJob.contains("developer")) {
            normalizedJob = "software-engineer";
        }

        JobBenchmark jobData = benchmarks.get(normalizedJob);
        if (jobData == null)
            return "Unknown";

        String normalizedCity = SlugNormalizer.normalize(citySlug);
        Optional<CityBenchmark> cityData = jobData.cities.stream()
                .filter(c -> SlugNormalizer.normalize(c.slug).equals(normalizedCity))
                .findFirst();

        if (cityData.isEmpty())
            return "Unknown";

        // Logic: Calculate roughly where 'salary' falls
        // p25 = 130k, p75 = 220k. Salary = 171k.
        // Interpolate.
        double p25 = cityData.get().percentile25;
        double p75 = cityData.get().percentile75;
        double median = cityData.get().medianSalary;

        // Simple linear interpolation for display purposes
        double percentile;
        if (salary < p25) {
            percentile = 10 + (salary / p25) * 15; // Bottom 25% (0-25 range)
        } else if (salary < median) {
            percentile = 25 + ((salary - p25) / (median - p25)) * 25; // 25-50 range
        } else if (salary < p75) {
            percentile = 50 + ((salary - median) / (p75 - median)) * 25; // 50-75 range
        } else {
            percentile = 75 + ((salary - p75) / (p75 * 0.5)) * 24; // 75-99 range
            if (percentile > 99)
                percentile = 99;
        }

        // "Top X%" = 100 - percentile
        int topX = 100 - (int) percentile;
        return String.valueOf(topX) + "%";
    }

    // Logic: Is this a good salary?
    public boolean isGoodSalary(String jobSlug, String citySlug, double salary) {
        // Same fallback logic
        String normalizedJob = SlugNormalizer.normalize(jobSlug);
        if (normalizedJob.contains("software") || normalizedJob.contains("engineer")
                || normalizedJob.contains("developer")) {
            normalizedJob = "software-engineer";
        }

        JobBenchmark jobData = benchmarks.get(normalizedJob);
        if (jobData == null)
            return true; // Benefit of the doubt

        String normalizedCity = SlugNormalizer.normalize(citySlug);
        return jobData.cities.stream()
                .filter(c -> SlugNormalizer.normalize(c.slug).equals(normalizedCity))
                .map(period -> salary >= period.medianSalary)
                .findFirst()
                .orElse(true);
    }

    // Inner Classes
    public static class JobBenchmark {
        public String title;
        public List<CityBenchmark> cities;
    }

    public static class CityBenchmark {
        public String slug;
        public double medianSalary;
        public double percentile25;
        public double percentile75;
    }
}
