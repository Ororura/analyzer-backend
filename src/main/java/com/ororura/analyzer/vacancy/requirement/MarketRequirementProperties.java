package com.ororura.analyzer.vacancy.requirement;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.resume.market")
public record MarketRequirementProperties(
        double minRequirementFrequency,
        int minVacancyCount,
        int minimumSampleSize,
        int batchSize,
        int maxRetries) {

    public MarketRequirementProperties {
        if (!Double.isFinite(minRequirementFrequency)
                || minRequirementFrequency < 0 || minRequirementFrequency > 1) {
            throw new IllegalArgumentException("Minimum requirement frequency must be between 0 and 1");
        }
        if (minVacancyCount < 1 || minimumSampleSize < 1 || batchSize < 1 || maxRetries < 0) {
            throw new IllegalArgumentException("Market requirement count and batch settings are invalid");
        }
    }
}
