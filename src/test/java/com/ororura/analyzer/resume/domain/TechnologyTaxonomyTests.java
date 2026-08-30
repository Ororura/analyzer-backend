package com.ororura.analyzer.resume.domain;

import java.time.Instant;
import java.util.List;
import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketRequirement;
import com.ororura.analyzer.resume.market.RequirementType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechnologyTaxonomyTests {

    private final TechnologyTaxonomy taxonomy = new TechnologyTaxonomy();
    private final MarketAnalysisProfile marketProfile = profile();

    @Test
    void mergesEvidenceByPrecedenceAndDeduplicatesAliases() {
        var profile = taxonomy.profile(marketProfile, "Used PostgreSQL and Docker",
                List.of("PostgreSQL"), List.of("Docker"), List.of("PostgreSQL", "Kubernetes"));
        assertThat(profile.confirmed()).contains("PostgreSQL");
        assertThat(profile.weakEvidence()).contains("Docker");
        assertThat(profile.missing()).containsExactly("Kubernetes");
    }

    @Test
    void treatsUnclassifiedTextMentionsAsWeakEvidence() {
        var profile = taxonomy.profile(marketProfile, "Skills: Java, Spring Boot", List.of(), List.of(), List.of());

        assertThat(profile.confirmed()).isEmpty();
        assertThat(profile.weakEvidence()).containsExactly("Java", "Spring Boot");
    }

    static MarketAnalysisProfile profile() {
        return new MarketAnalysisProfile(ResumeAnalysisProfile.JAVA_BACKEND, "Backend",
                List.of(new AnalysisCriterion("backend", "Backend", "Backend engineering", 1, 1)),
                List.of(requirement("postgresql", "PostgreSQL", .8), requirement("docker", "Docker", .6),
                        requirement("kubernetes", "Kubernetes", .4), requirement("java", "Java", .9),
                        requirement("spring-boot", "Spring Boot", .7)),
                10, Instant.EPOCH, "sha256:test");
    }

    private static MarketRequirement requirement(String id, String label, double frequency) {
        return new MarketRequirement(id, label, RequirementType.TECHNOLOGY, frequency);
    }
}
