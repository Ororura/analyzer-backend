package com.ororura.analyzer.resume.domain;

import java.util.List;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TechnologyTaxonomyTests {

    private final TechnologyTaxonomy taxonomy = new TechnologyTaxonomy();
    private final JavaBackendAnalysisProfile definition = new JavaBackendAnalysisProfile();

    @ParameterizedTest
    @CsvSource({
            "Postgres,PostgreSQL", "postgresql,PostgreSQL", "Spring Framework,Spring Boot",
            "Jakarta Persistence,Hibernate/JPA", "K8s,Kubernetes", "GitLab CI/CD,CI/CD"
    })
    void normalizesAliases(String input, String expected) {
        assertThat(taxonomy.canonical(definition, input)).isEqualTo(expected);
    }

    @Test
    void mergesEvidenceByPrecedenceAndDeduplicatesAliases() {
        var profile = taxonomy.profile(definition, "Used PostgreSQL and Docker",
                List.of("Postgres"), List.of("Docker"), List.of("postgresql", "K8s"));
        assertThat(profile.confirmed()).contains("PostgreSQL");
        assertThat(profile.weakEvidence()).contains("Docker");
        assertThat(profile.missing()).containsExactly("Kubernetes");
    }

    @Test
    void treatsUnclassifiedTextMentionsAsWeakEvidence() {
        var profile = taxonomy.profile(definition, "Skills: Java, Spring Boot", List.of(), List.of(), List.of());

        assertThat(profile.confirmed()).isEmpty();
        assertThat(profile.weakEvidence()).containsExactly("Java", "Spring Boot");
    }
}
