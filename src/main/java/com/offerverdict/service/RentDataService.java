package com.offerverdict.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerverdict.exception.ResourceNotFoundException;
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

@Service
public class RentDataService {

    private final ObjectMapper objectMapper;
    private Map<String, RentCityEntry> rentBySlug = Collections.emptyMap();

    public RentDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            RentDataContainer container = objectMapper.readValue(
                    new ClassPathResource("data/rent-data-2026.json").getInputStream(),
                    RentDataContainer.class);
            this.rentBySlug = container.cities.stream()
                    .collect(Collectors.toMap(c -> SlugNormalizer.normalize(c.slug), c -> c));
        } catch (IOException e) {
            System.err.println("Warning: Could not load rent-data-2026.json: " + e.getMessage());
        }
    }

    public double getMedianRent(String citySlug) {
        return findEntry(citySlug).map(e -> e.medianRent).orElse(2000.0); // Fallback
    }

    public Optional<RentCityEntry> findEntry(String citySlug) {
        return Optional.ofNullable(rentBySlug.get(SlugNormalizer.normalize(citySlug)));
    }

    // Inner Classes for JSON Mapping
    private static class RentDataContainer {
        public List<RentCityEntry> cities;
    }

    public static class RentCityEntry {
        public String slug;
        public String city;
        public double medianRent;
        public double avgRent1BR;
        public double avgRent2BR;
        public double yearOverYearChange;
        public String dataSource;
    }
}
