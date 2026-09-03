package com.ororura.analyzer.market.requirement;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.market.domain.MarketRequirement;
import com.ororura.analyzer.market.domain.RequirementType;

public record MarketRequirementStatistics(
        ResumeAnalysisProfile profile,
        int fetchedCount,
        int sampleSize,
        int processedCount,
        int failedCount,
        boolean sufficientSample,
        List<VacancyRequirements> vacancyRequirements,
        List<RequirementStatistics> requirements) {

    public MarketRequirementStatistics {
        if (profile == null || fetchedCount < 0 || processedCount < 0 || failedCount < 0
                || sampleSize != processedCount || fetchedCount != processedCount + failedCount) {
            throw new IllegalArgumentException("Market requirement sample accounting is inconsistent");
        }
        vacancyRequirements = List.copyOf(vacancyRequirements);
        requirements = List.copyOf(requirements);
        if (vacancyRequirements.size() != processedCount) {
            throw new IllegalArgumentException("Vacancy requirement mapping must cover every processed vacancy");
        }
        Set<String> vacancyIds = new HashSet<>();
        if (vacancyRequirements.stream().anyMatch(value -> !vacancyIds.add(value.vacancyId()))) {
            throw new IllegalArgumentException("Vacancy requirement mapping contains duplicate vacancy ids");
        }
        Set<String> requirementIds = requirements.stream()
                .map(RequirementStatistics::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (vacancyRequirements.stream().flatMap(value -> value.requirements().stream())
                .anyMatch(value -> !requirementIds.contains(value.requirementId()))) {
            throw new IllegalArgumentException("Vacancy mapping references unknown market requirement");
        }
    }

    public record VacancyRequirements(String vacancyId, List<MappedRequirement> requirements) {
        public VacancyRequirements {
            if (vacancyId == null || vacancyId.isBlank() || requirements == null) {
                throw new IllegalArgumentException("Vacancy requirement mapping is invalid");
            }
            requirements = List.copyOf(requirements);
        }
    }

    public record MappedRequirement(String requirementId, RequirementImportance importance) {
        public MappedRequirement {
            if (requirementId == null || requirementId.isBlank() || importance == null) {
                throw new IllegalArgumentException("Mapped market requirement is invalid");
            }
        }
    }

    public record RequirementStatistics(
            String id,
            String label,
            RequirementType type,
            int totalVacancyCount,
            int requiredCount,
            int preferredCount,
            int optionalCount,
            double frequency,
            double requiredFrequency,
            double preferredFrequency,
            double optionalFrequency) {

        public RequirementStatistics {
            if (id == null || id.isBlank() || label == null || label.isBlank() || type == null
                    || totalVacancyCount < 0 || requiredCount < 0 || preferredCount < 0 || optionalCount < 0
                    || totalVacancyCount != requiredCount + preferredCount + optionalCount) {
                throw new IllegalArgumentException("Market requirement statistics are inconsistent");
            }
            validateFrequency(frequency);
            validateFrequency(requiredFrequency);
            validateFrequency(preferredFrequency);
            validateFrequency(optionalFrequency);
        }

        public MarketRequirement requirement() {
            return new MarketRequirement(id, label, type, frequency);
        }

        private static void validateFrequency(double value) {
            if (!Double.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Market requirement frequency must be between 0 and 1");
            }
        }
    }
}
