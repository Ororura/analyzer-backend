package com.ororura.analyzer.market.domain;

import java.time.Instant;
import java.util.List;

import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketAnalysisProfileFactoryTests {

    private final MarketAnalysisProfileFactory factory = new MarketAnalysisProfileFactory();

    @Test
    void fingerprintIsDeterministicAndDoesNotDependOnGenerationTime() {
        MarketAnalysisProfile first = profile(criterion(0.5), requirement(0.8), 20, Instant.EPOCH);
        MarketAnalysisProfile second = profile(criterion(0.5), requirement(0.8), 20,
                Instant.parse("2026-08-30T10:00:00Z"));

        assertThat(first.version()).startsWith("sha256:").hasSize(71);
        assertThat(second.version()).isEqualTo(first.version());
    }

    @Test
    void fingerprintChangesWithScoringRequirementsAndSample() {
        MarketAnalysisProfile baseline = profile(criterion(0.5), requirement(0.8), 20, Instant.EPOCH);

        assertThat(profile(criterion(0.6), requirement(0.8), 20, Instant.EPOCH).version())
                .isNotEqualTo(baseline.version());
        assertThat(profile(criterion(0.5), requirement(0.9), 20, Instant.EPOCH).version())
                .isNotEqualTo(baseline.version());
        assertThat(profile(criterion(0.5), requirement(0.8), 21, Instant.EPOCH).version())
                .isNotEqualTo(baseline.version());
    }

    private MarketAnalysisProfile profile(AnalysisCriterion criterion, MarketRequirement requirement,
            int sampleSize, Instant generatedAt) {
        return factory.create(ResumeAnalysisProfile.JAVA_BACKEND, "Java Backend Developer",
                List.of(criterion), List.of(requirement), sampleSize, generatedAt);
    }

    private static AnalysisCriterion criterion(double frequency) {
        return new AnalysisCriterion("java", "Java", "Java depth", 1, frequency);
    }

    private static MarketRequirement requirement(double frequency) {
        return new MarketRequirement("java", "Java", RequirementType.TECHNOLOGY, frequency);
    }
}
