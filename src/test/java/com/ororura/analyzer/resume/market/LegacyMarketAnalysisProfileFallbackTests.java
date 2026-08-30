package com.ororura.analyzer.resume.market;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;
import com.ororura.analyzer.resume.ai.profile.ReactFrontendAnalysisProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMarketAnalysisProfileFallbackTests {

    @Test
    void createsVersionedIsolatedFallbackProfiles() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"), ZoneOffset.UTC);
        ResumeAnalysisProfileRegistry registry = new ResumeAnalysisProfileRegistry(List.of(
                new JavaBackendAnalysisProfile(), new ReactFrontendAnalysisProfile()));
        LegacyMarketAnalysisProfileFallback fallback = new LegacyMarketAnalysisProfileFallback(
                registry, new MarketAnalysisProfileFactory(), clock);

        MarketAnalysisProfile java = fallback.get(ResumeAnalysisProfile.JAVA_BACKEND);
        MarketAnalysisProfile react = fallback.get(ResumeAnalysisProfile.REACT_FRONTEND);

        assertThat(java.sampleSize()).isZero();
        assertThat(java.generatedAt()).isEqualTo(clock.instant());
        assertThat(java.version()).startsWith("sha256:");
        assertThat(java.criteria()).allSatisfy(criterion -> assertThat(criterion.marketFrequency()).isZero());
        assertThat(java.requirements()).filteredOn(value -> value.label().equals("Java"))
                .singleElement().satisfies(value -> {
                    assertThat(value.id()).isEqualTo("technology:java");
                    assertThat(value.type()).isEqualTo(RequirementType.TECHNOLOGY);
                    assertThat(value.frequency()).isEqualTo(0.93);
                });
        assertThat(react.requirements()).extracting(MarketRequirement::label).contains("React").doesNotContain("Java");
        assertThat(react.version()).isNotEqualTo(java.version());
        assertThat(fallback.get(ResumeAnalysisProfile.JAVA_BACKEND)).isSameAs(java);
    }
}
