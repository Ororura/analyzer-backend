package com.ororura.analyzer.vacancy.requirement.generation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;
import com.ororura.analyzer.resume.ai.profile.ReactFrontendAnalysisProfile;
import com.ororura.analyzer.resume.market.InMemoryMarketAnalysisProfileStore;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileFactory;
import com.ororura.analyzer.resume.market.MarketRequirement;
import com.ororura.analyzer.resume.market.RequirementType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketCriterionGeneratorTests {

    private final MarketCriterionProperties properties = new MarketCriterionProperties(6, 10);
    private final MarketCriterionAiGateway gateway = mock(MarketCriterionAiGateway.class);

    @Test
    void createsVersionedProfileWithoutExposingWeightsToLlm() {
        when(gateway.complete(any())).thenReturn(MarketCriterionFixture.validResponse());
        MarketCriterionGenerator generator = generator(Instant.parse("2026-08-30T10:00:00Z"));

        MarketAnalysisProfile profile = generator.generate(MarketCriterionFixture.statistics());

        assertThat(profile.criteria()).hasSize(6);
        assertThat(profile.criteria().stream().mapToDouble(AnalysisCriterion::weight).sum())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(profile.requirements()).extracting(MarketRequirement::label).contains("SonarQube");
        assertThat(profile.criteria()).extracting(AnalysisCriterion::label).doesNotContain("SonarQube");
        assertThat(profile.version()).startsWith("sha256:");
    }

    @Test
    void identicalContentKeepsFingerprintAndModifiedMarketChangesIt() {
        when(gateway.complete(any())).thenReturn(MarketCriterionFixture.validResponse());
        MarketAnalysisProfile first = generator(Instant.EPOCH).generate(MarketCriterionFixture.statistics());
        MarketAnalysisProfile later = generator(Instant.parse("2026-08-30T10:00:00Z"))
                .generate(MarketCriterionFixture.statistics());
        MarketAnalysisProfile modified = generator(Instant.EPOCH)
                .generate(MarketCriterionFixture.statistics(true));

        assertThat(later.generatedAt()).isNotEqualTo(first.generatedAt());
        assertThat(later.version()).isEqualTo(first.version());
        assertThat(modified.version()).isNotEqualTo(first.version());
    }

    @Test
    void failedGenerationDoesNotReplaceCurrentProfile() {
        InMemoryMarketAnalysisProfileStore store = new InMemoryMarketAnalysisProfileStore();
        MarketAnalysisProfile current = new MarketAnalysisProfileFactory().create(
                ResumeAnalysisProfile.JAVA_BACKEND, "Java Backend Developer",
                List.of(new AnalysisCriterion("legacy", "Legacy", "Legacy", 1, 1)),
                List.of(new MarketRequirement("technology:java", "Java", RequirementType.TECHNOLOGY, 1)),
                1, Instant.EPOCH);
        store.publish(current);
        when(gateway.complete(any())).thenReturn("malformed");
        MarketAnalysisProfileGenerationService service = new MarketAnalysisProfileGenerationService(
                generator(Instant.EPOCH), store);

        assertThatThrownBy(() -> service.generateAndPublish(MarketCriterionFixture.statistics()))
                .isInstanceOf(MarketCriterionGenerationException.class);
        assertThat(store.current(ResumeAnalysisProfile.JAVA_BACKEND)).containsSame(current);
        assertThat(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).isEmpty();
    }

    private MarketCriterionGenerator generator(Instant instant) {
        ObjectMapper objectMapper = new ObjectMapper();
        ResumeAnalysisProfileRegistry registry = new ResumeAnalysisProfileRegistry(List.of(
                new JavaBackendAnalysisProfile(), new ReactFrontendAnalysisProfile()));
        MarketCriterionSchemaFactory schemaFactory = new MarketCriterionSchemaFactory(objectMapper, properties);
        return new MarketCriterionGenerator(registry,
                new MarketCriterionPromptFactory(objectMapper, schemaFactory), gateway,
                new MarketCriterionResponseParser(objectMapper),
                new MarketCriterionGroupingValidator(properties),
                new CriterionWeightCalculator(new CriterionIdFactory()),
                new MarketAnalysisProfileFactory(), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
