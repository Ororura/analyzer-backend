package com.ororura.analyzer.vacancy.requirement;

import java.util.List;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.market.RequirementType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VacancyRequirementAggregatorTests {

    @Test
    void deduplicatesPerVacancyAndKeepsImportanceStatisticsSeparate() {
        VacancyRequirementAggregator aggregator = new VacancyRequirementAggregator(properties(0, 1, 3));
        CanonicalRequirement required = requirement("java", RequirementImportance.REQUIRED);
        CanonicalRequirement preferred = requirement("java", RequirementImportance.PREFERRED);
        CanonicalRequirement optional = requirement("java", RequirementImportance.OPTIONAL);

        MarketRequirementStatistics result = aggregator.aggregate(ResumeAnalysisProfile.JAVA_BACKEND, 3, List.of(
                vacancy("v1", required, required, optional),
                vacancy("v2", preferred),
                vacancy("v3", optional)));

        assertThat(result.fetchedCount()).isEqualTo(3);
        assertThat(result.processedCount()).isEqualTo(3);
        assertThat(result.failedCount()).isZero();
        assertThat(result.sufficientSample()).isTrue();
        assertThat(result.requirements()).singleElement().satisfies(value -> {
            assertThat(value.totalVacancyCount()).isEqualTo(3);
            assertThat(value.requiredCount()).isEqualTo(1);
            assertThat(value.preferredCount()).isEqualTo(1);
            assertThat(value.optionalCount()).isEqualTo(1);
            assertThat(value.frequency()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-12));
            assertThat(value.requiredFrequency()).isCloseTo(1.0 / 3, org.assertj.core.data.Offset.offset(1e-12));
            assertThat(value.preferredFrequency()).isCloseTo(1.0 / 3, org.assertj.core.data.Offset.offset(1e-12));
            assertThat(value.optionalFrequency()).isCloseTo(1.0 / 3, org.assertj.core.data.Offset.offset(1e-12));
        });
    }

    @Test
    void usesProcessedVacanciesAsDenominatorAndAccountsFailures() {
        VacancyRequirementAggregator aggregator = new VacancyRequirementAggregator(properties(0, 1, 5));
        MarketRequirementStatistics result = aggregator.aggregate(ResumeAnalysisProfile.JAVA_BACKEND, 4, List.of(
                vacancy("v1", requirement("java", RequirementImportance.REQUIRED)),
                vacancy("v2")));

        assertThat(result.sampleSize()).isEqualTo(2);
        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.failedCount()).isEqualTo(2);
        assertThat(result.sufficientSample()).isFalse();
        assertThat(result.requirements()).singleElement()
                .extracting(MarketRequirementStatistics.RequirementStatistics::frequency)
                .isEqualTo(0.5);
    }

    @Test
    void appliesMinimumFrequencyAndAbsoluteCountTogether() {
        List<CanonicalVacancyRequirements> sample = List.of(
                vacancy("v1", requirement("java", RequirementImportance.REQUIRED),
                        requirement("kafka", RequirementImportance.OPTIONAL)),
                vacancy("v2", requirement("java", RequirementImportance.REQUIRED)),
                vacancy("v3"), vacancy("v4"));

        MarketRequirementStatistics minCount = new VacancyRequirementAggregator(properties(0, 2, 1))
                .aggregate(ResumeAnalysisProfile.JAVA_BACKEND, 4, sample);
        assertThat(minCount.requirements()).extracting(MarketRequirementStatistics.RequirementStatistics::label)
                .containsExactly("Java");

        MarketRequirementStatistics minFrequency = new VacancyRequirementAggregator(properties(0.6, 1, 1))
                .aggregate(ResumeAnalysisProfile.JAVA_BACKEND, 4, sample);
        assertThat(minFrequency.requirements()).isEmpty();
    }

    private static MarketRequirementProperties properties(double minFrequency, int minCount, int minSample) {
        return new MarketRequirementProperties(minFrequency, minCount, minSample, 5, 1);
    }

    private static CanonicalVacancyRequirements vacancy(String id, CanonicalRequirement... requirements) {
        return new CanonicalVacancyRequirements(id, List.of(requirements));
    }

    private static CanonicalRequirement requirement(String label, RequirementImportance importance) {
        return new CanonicalRequirement("technology:" + label, label.substring(0, 1).toUpperCase() + label.substring(1),
                RequirementType.TECHNOLOGY, importance);
    }
}
