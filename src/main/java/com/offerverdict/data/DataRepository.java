package com.offerverdict.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.exception.ResourceNotFoundException;
import com.offerverdict.model.AuthoritativeMetrics;
import com.offerverdict.model.CityCostEntry;
import com.offerverdict.model.JobInfo;
import com.offerverdict.model.StateTax;
import com.offerverdict.model.TaxData;
import com.offerverdict.util.SlugNormalizer;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DataRepository {
    private final ObjectMapper objectMapper;
    private TaxData taxData;
    private List<CityCostEntry> cities = Collections.emptyList();
    private List<JobInfo> jobs = Collections.emptyList();
    private Map<String, CityCostEntry> cityBySlug = Collections.emptyMap();
    private Map<String, JobInfo> jobBySlug = Collections.emptyMap();
    private AuthoritativeMetrics authoritativeMetrics;

    public DataRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public synchronized void reload() {
        try {
            this.taxData = objectMapper.readValue(
                    new ClassPathResource("data/StateTax.json").getInputStream(),
                    TaxData.class);

            this.authoritativeMetrics = objectMapper.readValue(
                    new ClassPathResource("data/AuthoritativeData.json").getInputStream(),
                    AuthoritativeMetrics.class);

            // Updated to handle metadata wrapper
            CityDataContainer cityContainer = objectMapper.readValue(
                    new ClassPathResource("data/CityCost.json").getInputStream(),
                    CityDataContainer.class);
            this.cities = cityContainer.cities;

            this.jobs = objectMapper.readValue(
                    new ClassPathResource("data/Jobs.json").getInputStream(),
                    new TypeReference<>() {
                    });
            this.cityBySlug = cities.stream()
                    .collect(Collectors.toMap(c -> SlugNormalizer.normalize(c.getSlug()), c -> c));
            this.jobBySlug = jobs.stream()
                    .collect(Collectors.toMap(j -> SlugNormalizer.normalize(j.getSlug()), j -> j));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load JSON data", e);
        }
    }

    // Inner class for JSON wrapper
    private static class CityDataContainer {
        public List<CityCostEntry> cities;
    }

    public TaxData getTaxData() {
        return taxData;
    }

    public CityCostEntry getCity(String slug) {
        return Optional.ofNullable(cityBySlug.get(SlugNormalizer.normalize(slug)))
                .orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + slug));
    }

    public Optional<CityCostEntry> findCityLoosely(String slug) {
        String normalized = SlugNormalizer.normalize(slug);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        if (cityBySlug.containsKey(normalized)) {
            return Optional.of(cityBySlug.get(normalized));
        }

        Optional<CityCostEntry> startsWith = cities.stream()
                .filter(city -> {
                    String citySlug = SlugNormalizer.normalize(city.getSlug());
                    return citySlug.startsWith(normalized) || normalized.startsWith(citySlug);
                })
                .min(Comparator.comparingInt(
                        city -> Math.abs(SlugNormalizer.normalize(city.getSlug()).length() - normalized.length())));

        if (startsWith.isPresent()) {
            return startsWith;
        }

        return cities.stream()
                .filter(city -> {
                    String citySlug = SlugNormalizer.normalize(city.getSlug());
                    return citySlug.contains(normalized) || normalized.contains(citySlug);
                })
                .min(Comparator.comparingInt(
                        city -> Math.abs(SlugNormalizer.normalize(city.getSlug()).length() - normalized.length())));
    }

    public JobInfo getJob(String slug) {
        return Optional.ofNullable(jobBySlug.get(SlugNormalizer.normalize(slug)))
                .orElseThrow(() -> new ResourceNotFoundException("Unknown job slug: " + slug));
    }

    public Optional<JobInfo> findJobLoosely(String slug) {
        String normalized = SlugNormalizer.normalize(slug);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        if (jobBySlug.containsKey(normalized)) {
            return Optional.of(jobBySlug.get(normalized));
        }

        Optional<JobInfo> startsWith = jobs.stream()
                .filter(job -> {
                    String jobSlug = SlugNormalizer.normalize(job.getSlug());
                    return jobSlug.startsWith(normalized) || normalized.startsWith(jobSlug);
                })
                .min(Comparator.comparingInt(
                        job -> Math.abs(SlugNormalizer.normalize(job.getSlug()).length() - normalized.length())));

        if (startsWith.isPresent()) {
            return startsWith;
        }

        return jobs.stream()
                .filter(job -> {
                    String jobSlug = SlugNormalizer.normalize(job.getSlug());
                    return jobSlug.contains(normalized) || normalized.contains(jobSlug);
                })
                .min(Comparator.comparingInt(
                        job -> Math.abs(SlugNormalizer.normalize(job.getSlug()).length() - normalized.length())));
    }

    public boolean hasCity(String slug) {
        return cityBySlug.containsKey(SlugNormalizer.normalize(slug));
    }

    public boolean hasJob(String slug) {
        return jobBySlug.containsKey(SlugNormalizer.normalize(slug));
    }

    public List<CityCostEntry> getCities() {
        return cities;
    }

    public List<CityCostEntry> getRelatedCities(String state, String currentCitySlug, int limit) {
        return cities.stream()
                .filter(c -> c.getState().equalsIgnoreCase(state))
                .filter(c -> !SlugNormalizer.normalize(c.getSlug()).equals(SlugNormalizer.normalize(currentCitySlug)))
                .sorted(Comparator.comparing(CityCostEntry::getCity))
                .limit(limit)
                .toList();
    }

    public List<JobInfo> getJobs() {
        return jobs.stream()
                .sorted(Comparator.comparing(JobInfo::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Map<String, StateTax> stateTaxMap() {
        return taxData.getStates().stream()
                .collect(Collectors.toMap(s -> s.getState().toUpperCase(Locale.US), s -> s));
    }

    public AuthoritativeMetrics getAuthoritativeMetrics() {
        return authoritativeMetrics;
    }
}
