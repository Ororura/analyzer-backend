package com.ororura.analyzer.resume.market;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

public record MarketAnalysisProfile(
        ResumeAnalysisProfile profile,
        String targetRole,
        List<AnalysisCriterion> criteria,
        List<MarketRequirement> requirements,
        List<MarketSkillStatistics> skillStatistics,
        List<MarketVacancyRequirements> vacancyRequirements,
        boolean sufficientSample,
        int sampleSize,
        Instant generatedAt,
        String version) {

    public MarketAnalysisProfile {
        if (targetRole == null || targetRole.isBlank()) {
            throw new IllegalArgumentException("Market analysis target role must not be blank");
        }
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("Market analysis criteria must not be empty");
        }
        if (requirements == null) {
            throw new IllegalArgumentException("Market analysis requirements must be defined");
        }
        if (sampleSize < 0) {
            throw new IllegalArgumentException("Market analysis sample size must be non-negative");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Market analysis generation time must be defined");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Market analysis version must not be blank");
        }
        criteria = List.copyOf(criteria);
        requirements = List.copyOf(requirements);
        skillStatistics = skillStatistics == null ? List.of() : List.copyOf(skillStatistics);
        vacancyRequirements = vacancyRequirements == null ? List.of() : List.copyOf(vacancyRequirements);
        ensureUniqueCriterionIds(criteria);
    }

    public MarketAnalysisProfile(ResumeAnalysisProfile profile, String targetRole,
            List<AnalysisCriterion> criteria, List<MarketRequirement> requirements,
            int sampleSize, Instant generatedAt, String version) {
        this(profile, targetRole, criteria, requirements, List.of(), List.of(), false,
                sampleSize, generatedAt, version);
        if (profile == null) throw new IllegalArgumentException("Legacy profile type must be defined");
    }

    private static void ensureUniqueCriterionIds(List<AnalysisCriterion> criteria) {
        Set<String> ids = new HashSet<>();
        for (AnalysisCriterion criterion : criteria) {
            if (!ids.add(criterion.id())) {
                throw new IllegalArgumentException("Duplicate market analysis criterion id: " + criterion.id());
            }
        }
    }
}
