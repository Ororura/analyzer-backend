package com.ororura.analyzer.market.requirement;

import java.util.List;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.analysis.profile.definition.JavaBackendAnalysisProfile;
import com.ororura.analyzer.analysis.profile.definition.ReactFrontendAnalysisProfile;
import com.ororura.analyzer.market.domain.RequirementType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementCanonicalizerTests {

    private final RequirementCanonicalizer canonicalizer = new RequirementCanonicalizer(
            new com.ororura.analyzer.analysis.profile.DefaultAnalysisTechnologyCatalog());

    @Test
    void canonicalizesAliasesAndCaseDeterministically() {
        assertThat(canonical("Postgres").label()).isEqualTo("PostgreSQL");
        assertThat(canonical("postgres").id()).isEqualTo("technology:postgresql");
        assertThat(canonical("POSTGRESQL").label()).isEqualTo("PostgreSQL");

        CanonicalRequirement js = canonicalizer.canonicalize(ResumeAnalysisProfile.REACT_FRONTEND,
                requirement("ECMAScript", RequirementType.TECHNOLOGY, RequirementImportance.REQUIRED));
        assertThat(js.label()).isEqualTo("JavaScript");
        assertThat(canonicalizer.canonicalize(ResumeAnalysisProfile.REACT_FRONTEND,
                requirement("js", RequirementType.TECHNOLOGY, RequirementImportance.REQUIRED)).id())
                .isEqualTo(js.id());
    }

    @Test
    void keepsSpringVariantsSeparateAndPreservesUnknownRequirements() {
        assertThat(canonical("Spring").id()).isEqualTo("technology:spring");
        assertThat(canonical("Spring Boot").id()).isEqualTo("technology:spring-boot");
        assertThat(canonical("Spring Security").id()).isEqualTo("technology:spring-security");

        CanonicalRequirement unknown = canonicalizer.canonicalize(ResumeAnalysisProfile.JAVA_BACKEND,
                requirement("Production Troubleshooting", RequirementType.PRACTICE,
                        RequirementImportance.PREFERRED));
        assertThat(unknown.id()).isEqualTo("practice:production-troubleshooting");
        assertThat(unknown.label()).isEqualTo("Production troubleshooting");
    }

    private CanonicalRequirement canonical(String label) {
        return canonicalizer.canonicalize(ResumeAnalysisProfile.JAVA_BACKEND,
                requirement(label, RequirementType.TECHNOLOGY, RequirementImportance.REQUIRED));
    }

    private static ExtractedRequirement requirement(String label, RequirementType type,
            RequirementImportance importance) {
        return new ExtractedRequirement(label, type, importance);
    }
}
