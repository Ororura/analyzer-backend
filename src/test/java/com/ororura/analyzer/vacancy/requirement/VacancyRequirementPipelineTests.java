package com.ororura.analyzer.vacancy.requirement;

import java.util.List;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;
import com.ororura.analyzer.resume.ai.profile.ReactFrontendAnalysisProfile;
import com.ororura.analyzer.resume.market.RequirementType;
import com.ororura.analyzer.vacancy.model.Vacancy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VacancyRequirementPipelineTests {

    private final ResumeAnalysisProfileRegistry registry = new ResumeAnalysisProfileRegistry(List.of(
            new JavaBackendAnalysisProfile(), new ReactFrontendAnalysisProfile()));
    private final RequirementCanonicalizer canonicalizer = new RequirementCanonicalizer(registry);
    private final KnownRequirementExtractor knownExtractor = new KnownRequirementExtractor(registry, canonicalizer);
    private final VacancyRequirementMerger merger = new VacancyRequirementMerger(canonicalizer);

    @Test
    void mergesRegexAndSemanticExtractionAndDeduplicatesMentions() {
        VacancyRequirementExtractor semantic = mock(VacancyRequirementExtractor.class);
        when(semantic.extract(anyList())).thenAnswer(invocation -> {
            List<VacancyRequirementInput> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> new ExtractedVacancyRequirements(input.vacancyId(), List.of(
                    extracted("Java", RequirementImportance.REQUIRED),
                    extracted("java", RequirementImportance.OPTIONAL),
                    extracted("Postgres", RequirementImportance.PREFERRED),
                    extracted("PostgreSQL", RequirementImportance.PREFERRED))))
                    .toList();
        });
        VacancyRequirementPipeline pipeline = pipeline(semantic, properties(0, 1, 1, 5, 0));

        MarketRequirementStatistics result = pipeline.analyze(ResumeAnalysisProfile.JAVA_BACKEND,
                List.of(vacancy("v1", "Java Developer", "Java Java Java PostgreSQL")));

        assertThat(result.requirements()).extracting(MarketRequirementStatistics.RequirementStatistics::label)
                .containsExactlyInAnyOrder("Java", "PostgreSQL");
        assertThat(result.requirements()).allSatisfy(value -> {
            assertThat(value.totalVacancyCount()).isEqualTo(1);
            assertThat(value.frequency()).isEqualTo(1.0);
        });
        assertThat(result.requirements()).filteredOn(value -> value.label().equals("Java"))
                .singleElement().extracting(MarketRequirementStatistics.RequirementStatistics::requiredCount)
                .isEqualTo(1);
    }

    @Test
    void isolatesFailedVacancyAfterBoundedBatchRetries() {
        VacancyRequirementExtractor semantic = mock(VacancyRequirementExtractor.class);
        when(semantic.extract(anyList())).thenAnswer(invocation -> {
            List<VacancyRequirementInput> inputs = invocation.getArgument(0);
            if (inputs.stream().anyMatch(value -> value.vacancyId().equals("bad"))) {
                throw new VacancyRequirementExtractionException("malformed", null);
            }
            return inputs.stream().map(input -> new ExtractedVacancyRequirements(input.vacancyId(),
                    List.of(extracted("Java", RequirementImportance.REQUIRED)))).toList();
        });
        VacancyRequirementPipeline pipeline = pipeline(semantic, properties(0, 1, 1, 2, 1));

        MarketRequirementStatistics result = pipeline.analyze(ResumeAnalysisProfile.JAVA_BACKEND, List.of(
                vacancy("good", "Java Developer", "Java"),
                vacancy("bad", "Broken vacancy", "Malformed extraction")));

        assertThat(result.fetchedCount()).isEqualTo(2);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.requirements()).singleElement()
                .extracting(MarketRequirementStatistics.RequirementStatistics::frequency)
                .isEqualTo(1.0);
    }

    @Test
    void processesEmptyDescriptionUsingRegexTitleFastPath() {
        VacancyRequirementExtractor semantic = mock(VacancyRequirementExtractor.class);
        when(semantic.extract(anyList())).thenAnswer(invocation -> {
            List<VacancyRequirementInput> inputs = invocation.getArgument(0);
            return inputs.stream().map(input -> new ExtractedVacancyRequirements(
                    input.vacancyId(), List.of())).toList();
        });
        VacancyRequirementPipeline pipeline = pipeline(semantic, properties(0, 1, 1, 5, 0));

        MarketRequirementStatistics result = pipeline.analyze(ResumeAnalysisProfile.JAVA_BACKEND,
                List.of(vacancy("v1", "Java Developer", null)));

        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.requirements()).extracting(MarketRequirementStatistics.RequirementStatistics::label)
                .contains("Java");
    }

    private VacancyRequirementPipeline pipeline(VacancyRequirementExtractor semantic,
            MarketRequirementProperties properties) {
        return new VacancyRequirementPipeline(semantic, knownExtractor, merger,
                new VacancyRequirementAggregator(properties), properties);
    }

    private static MarketRequirementProperties properties(double minFrequency, int minCount,
            int minSample, int batchSize, int retries) {
        return new MarketRequirementProperties(minFrequency, minCount, minSample, batchSize, retries);
    }

    private static ExtractedRequirement extracted(String label, RequirementImportance importance) {
        return new ExtractedRequirement(label, RequirementType.TECHNOLOGY, importance);
    }

    private static Vacancy vacancy(String id, String title, String description) {
        return new Vacancy(id, id, title, "Company", null, null, null, null, description,
                List.of(), List.of(), List.of(), null, null, null, null, "test", null, null);
    }
}
