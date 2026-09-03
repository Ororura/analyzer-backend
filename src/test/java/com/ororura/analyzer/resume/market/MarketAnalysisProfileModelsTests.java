package com.ororura.analyzer.market.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketAnalysisProfileModelsTests {

    @Test
    void validatesMarketRequirement() {
        assertThat(new MarketRequirement("java", "Java", RequirementType.TECHNOLOGY, 0).frequency()).isZero();
        assertThat(new MarketRequirement("java", "Java", RequirementType.TECHNOLOGY, 1).frequency()).isEqualTo(1);

        for (double frequency : List.of(-0.01, 1.01, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThatThrownBy(() -> new MarketRequirement(
                    "java", "Java", RequirementType.TECHNOLOGY, frequency))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new MarketRequirement(" ", "Java", RequirementType.TECHNOLOGY, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MarketRequirement("java", " ", RequirementType.TECHNOLOGY, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MarketRequirement("java", "Java", null, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesMarketDrivenCriterionAndKeepsLegacyConstructor() {
        AnalysisCriterion zeroWeight = new AnalysisCriterion("java", "Java", "Java depth", 0, 0.5);
        assertThat(zeroWeight.weight()).isZero();
        assertThat(new AnalysisCriterion("legacy", "Legacy", "Legacy criterion", 1).marketFrequency()).isZero();

        for (double weight : List.of(-0.01, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThatThrownBy(() -> new AnalysisCriterion(
                    "java", "Java", "Java depth", weight, 0.5))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        for (double frequency : List.of(-0.01, 1.01, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThatThrownBy(() -> new AnalysisCriterion(
                    "java", "Java", "Java depth", 1, frequency))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new AnalysisCriterion(" ", "Java", "Java depth", 1, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnalysisCriterion("java", " ", "Java depth", 1, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnalysisCriterion("java", "Java", " ", 1, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructsImmutableProfileAndRejectsDuplicateCriterionIds() {
        List<AnalysisCriterion> criteria = new ArrayList<>(List.of(criterion("java")));
        List<MarketRequirement> requirements = new ArrayList<>(List.of(requirement("java", 0.8)));
        MarketAnalysisProfile profile = new MarketAnalysisProfile(
                ResumeAnalysisProfile.JAVA_BACKEND, "Java Backend Developer", criteria, requirements,
                20, Instant.parse("2026-08-30T10:00:00Z"), "sha256:test");

        criteria.clear();
        requirements.clear();
        assertThat(profile.criteria()).hasSize(1);
        assertThat(profile.requirements()).hasSize(1);
        assertThatThrownBy(() -> profile.criteria().add(criterion("spring")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new MarketAnalysisProfile(
                ResumeAnalysisProfile.JAVA_BACKEND, "Java Backend Developer",
                List.of(criterion("java"), criterion("java")), List.of(), 0,
                Instant.EPOCH, "sha256:test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void validatesProfileConstruction() {
        assertThatThrownBy(() -> profile(null, "Role", 0, Instant.EPOCH, "v"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile(ResumeAnalysisProfile.JAVA_BACKEND, " ", 0, Instant.EPOCH, "v"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile(ResumeAnalysisProfile.JAVA_BACKEND, "Role", -1, Instant.EPOCH, "v"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile(ResumeAnalysisProfile.JAVA_BACKEND, "Role", 0, null, "v"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile(ResumeAnalysisProfile.JAVA_BACKEND, "Role", 0, Instant.EPOCH, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MarketAnalysisProfile(
                ResumeAnalysisProfile.JAVA_BACKEND, "Role", List.of(), List.of(), 0, Instant.EPOCH, "v"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MarketAnalysisProfile profile(ResumeAnalysisProfile profile, String role,
            int sampleSize, Instant generatedAt, String version) {
        return new MarketAnalysisProfile(profile, role, List.of(criterion("java")), List.of(),
                sampleSize, generatedAt, version);
    }

    static AnalysisCriterion criterion(String id) {
        return new AnalysisCriterion(id, id, id + " description", 1, 0.5);
    }

    static MarketRequirement requirement(String id, double frequency) {
        return new MarketRequirement(id, id, RequirementType.TECHNOLOGY, frequency);
    }
}
