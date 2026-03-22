package com.offerverdict.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RoleGuideService {

    private final Map<String, RoleGuide> guidesBySlug;

    public RoleGuideService() {
        this.guidesBySlug = buildGuides();
    }

    public List<RoleGuide> featuredGuides() {
        return List.copyOf(guidesBySlug.values());
    }

    public Optional<RoleGuide> findGuide(String jobSlug) {
        return Optional.ofNullable(guidesBySlug.get(jobSlug));
    }

    private Map<String, RoleGuide> buildGuides() {
        Map<String, RoleGuide> guides = new LinkedHashMap<>();

        guides.put("registered-nurse", new RoleGuide(
                "registered-nurse",
                "Registered Nurse",
                "High-mobility hospital and clinic offers where shift premiums, rent, and take-home pay change together.",
                "Healthcare relocation",
                "Move only if the raise survives rent, taxes, and shift fatigue.",
                List.of(
                        "Check whether the new hospital pay band beats the local rent burden.",
                        "Compare sign-on incentives against recurring monthly costs, not just year-one cash.",
                        "Stress-test your take-home pay in cities where overtime or night-shift premiums matter.")));

        guides.put("accountant", new RoleGuide(
                "accountant",
                "Accountant",
                "Public, industry, and controllership moves where title changes can hide weak real compensation.",
                "Finance transitions",
                "A better title is only worth it if your monthly residual actually improves.",
                List.of(
                        "Compare public vs industry offers after tax and commute-heavy metro costs.",
                        "Use the city benchmark to see whether a promotion still leaves room to save.",
                        "Check whether a modest raise in a lower-cost city beats a bigger salary in a pricier one.")));

        guides.put("teacher", new RoleGuide(
                "teacher",
                "Teacher",
                "District-to-district moves where salary schedules, housing costs, and state taxes all shift.",
                "District move planning",
                "Do not evaluate a district move from salary schedule alone.",
                List.of(
                        "Check whether the district step increase survives local housing costs.",
                        "Compare metro vs suburban districts on take-home pay, not posted salary alone.",
                        "Use the benchmark salary as a fast reality check before applying or moving.")));

        guides.put("project-manager", new RoleGuide(
                "project-manager",
                "Project Manager",
                "Cross-city role changes where compensation, commute, and management scope all move at once.",
                "Operations and PM relocation",
                "Higher scope is not a win if the city upgrade erases your cash-flow gain.",
                List.of(
                        "Compare program or PM offers when rent and commute both increase.",
                        "Stress-test whether the new city still leaves room for savings after tax.",
                        "Use relocation comparisons to quantify the real value of a title bump.")));

        guides.put("marketing-manager", new RoleGuide(
                "marketing-manager",
                "Marketing Manager",
                "Brand, growth, and performance roles where a bigger salary can disappear in higher-cost hubs.",
                "Commercial role comparison",
                "A stronger brand name is not enough if the city premium wipes out the raise.",
                List.of(
                        "Compare agency, in-house, and growth offers across expensive metros.",
                        "Check whether a new title still improves monthly residual after rent.",
                        "Use city benchmarks before accepting a role in a prestige market.")));

        guides.put("pharmacist", new RoleGuide(
                "pharmacist",
                "Pharmacist",
                "Hospital, retail, and ambulatory offers where city costs and shift structure materially affect the real outcome.",
                "Healthcare pay comparison",
                "The best pharmacy offer is the one that holds up after taxes and local costs.",
                List.of(
                        "Compare hospital vs retail offers across cities with different housing pressure.",
                        "Check whether the raise offsets cost-of-living and tax differences.",
                        "Use role-specific salary checks before committing to a relocation.")));

        guides.put("software-engineer", new RoleGuide(
                "software-engineer",
                "Software Engineer",
                "Tech offers where equity, rent, and state-tax differences can change the real value of compensation.",
                "Tech offer comparison",
                "Use the engine when the headline salary looks strong but the city shift is expensive.",
                List.of(
                        "Compare remote-friendly tech hubs against higher-salary expensive cities.",
                        "Use role benchmarks before assuming a raise is really a raise.",
                        "Check monthly residual and negotiation thresholds before signing.")));

        return guides;
    }

    public record RoleGuide(
            String slug,
            String title,
            String summary,
            String eyebrow,
            String decisionAngle,
            List<String> checkList) {
    }
}
