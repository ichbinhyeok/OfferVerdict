package com.offerverdict.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.exception.ResourceNotFoundException;
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
                    TaxData.class
            );
            this.cities = objectMapper.readValue(
                    new ClassPathResource("data/CityCost.json").getInputStream(),
                    new TypeReference<>() {
                    }
            );
            this.jobs = objectMapper.readValue(
                    new ClassPathResource("data/Jobs.json").getInputStream(),
                    new TypeReference<>() {
                    }
            );
            this.cityBySlug = cities.stream()
                    .collect(Collectors.toMap(c -> SlugNormalizer.normalize(c.getSlug()), c -> c));
            this.jobBySlug = jobs.stream()
                    .collect(Collectors.toMap(j -> SlugNormalizer.normalize(j.getSlug()), j -> j));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load JSON data", e);
        }
    }

    public TaxData getTaxData() {
        return taxData;
    }

    public CityCostEntry getCity(String slug) {
        return Optional.ofNullable(cityBySlug.get(SlugNormalizer.normalize(slug)))
                .orElseThrow(() -> new ResourceNotFoundException("Unknown city slug: " + slug));
    }

    public JobInfo getJob(String slug) {
        return Optional.ofNullable(jobBySlug.get(SlugNormalizer.normalize(slug)))
                .orElseThrow(() -> new ResourceNotFoundException("Unknown job slug: " + slug));
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

    public List<JobInfo> getJobs() {
        return jobs.stream()
                .sorted(Comparator.comparing(JobInfo::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Map<String, StateTax> stateTaxMap() {
        return taxData.getStates().stream()
                .collect(Collectors.toMap(s -> s.getState().toUpperCase(Locale.US), s -> s));
    }
}
