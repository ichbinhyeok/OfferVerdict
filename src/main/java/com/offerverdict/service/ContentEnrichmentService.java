package com.offerverdict.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for loading and providing enriched contextual content for cities and
 * jobs.
 * This content adds SEO value by providing unique, detailed information beyond
 * basic calculations.
 * 
 * Pattern: Follows same structure as DataRepository for consistency.
 */
@Service
public class ContentEnrichmentService {

    private final ObjectMapper objectMapper;
    private Map<String, CityContext> cityContexts = new HashMap<>();
    private Map<String, JobContext> jobContexts = new HashMap<>();

    public ContentEnrichmentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            loadCityContexts();
            loadJobContexts();
            System.out.println("✅ ContentEnrichmentService initialized: " +
                    cityContexts.size() + " cities, " +
                    jobContexts.size() + " jobs");
        } catch (IOException e) {
            System.err.println("⚠️  Warning: Could not load content enrichment data: " + e.getMessage());
            // Non-fatal: site will work without enrichment, just less SEO value
        }
    }

    private void loadCityContexts() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/city-context.json");
        cityContexts = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, CityContext>>() {
                });
    }

    private void loadJobContexts() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/job-context.json");
        jobContexts = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, JobContext>>() {
                });
    }

    /**
     * Get contextual information for a city by slug.
     * Returns empty Optional if city not found (graceful degradation).
     */
    public Optional<CityContext> getCityContext(String citySlug) {
        return Optional.ofNullable(cityContexts.get(citySlug));
    }

    /**
     * Get career context for a job by slug.
     * Returns empty Optional if job not found (graceful degradation).
     */
    public Optional<JobContext> getJobContext(String jobSlug) {
        return Optional.ofNullable(jobContexts.get(jobSlug));
    }

    // Inner classes for JSON deserialization

    public static class CityContext {
        private String jobMarketSummary;
        private String housingTrends;
        private String industryFocus;
        private String qualityOfLife;

        // Getters and setters
        public String getJobMarketSummary() {
            return jobMarketSummary;
        }

        public void setJobMarketSummary(String jobMarketSummary) {
            this.jobMarketSummary = jobMarketSummary;
        }

        public String getHousingTrends() {
            return housingTrends;
        }

        public void setHousingTrends(String housingTrends) {
            this.housingTrends = housingTrends;
        }

        public String getIndustryFocus() {
            return industryFocus;
        }

        public void setIndustryFocus(String industryFocus) {
            this.industryFocus = industryFocus;
        }

        public String getQualityOfLife() {
            return qualityOfLife;
        }

        public void setQualityOfLife(String qualityOfLife) {
            this.qualityOfLife = qualityOfLife;
        }
    }

    public static class JobContext {
        private String nationalOutlook;
        private String skillRequirements;
        private String careerGrowth;

        // Getters and setters
        public String getNationalOutlook() {
            return nationalOutlook;
        }

        public void setNationalOutlook(String nationalOutlook) {
            this.nationalOutlook = nationalOutlook;
        }

        public String getSkillRequirements() {
            return skillRequirements;
        }

        public void setSkillRequirements(String skillRequirements) {
            this.skillRequirements = skillRequirements;
        }

        public String getCareerGrowth() {
            return careerGrowth;
        }

        public void setCareerGrowth(String careerGrowth) {
            this.careerGrowth = careerGrowth;
        }
    }
}
