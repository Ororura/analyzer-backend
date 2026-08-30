package com.ororura.analyzer.vacancy.requirement;

import java.util.List;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketRequirement;
import com.ororura.analyzer.resume.market.RequirementType;

public record MarketRequirementStatistics(
        ResumeAnalysisProfile profile,
        int fetchedCount,
        int sampleSize,
        int processedCount,
        int failedCount,
        boolean sufficientSample,
        List<RequirementStatistics> requirements) {

    public MarketRequirementStatistics {
        if (profile == null || fetchedCount < 0 || processedCount < 0 || failedCount < 0
                || sampleSize != processedCount || fetchedCount != processedCount + failedCount) {
            throw new IllegalArgumentException("Market requirement sample accounting is inconsistent");
        }
        requirements = List.copyOf(requirements);
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
