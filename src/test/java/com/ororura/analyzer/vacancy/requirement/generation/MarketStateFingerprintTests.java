package com.ororura.analyzer.market.profile;

import java.util.List;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.market.domain.RequirementType;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics.MappedRequirement;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics.RequirementStatistics;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics.VacancyRequirements;
import com.ororura.analyzer.market.requirement.RequirementImportance;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketStateFingerprintTests {
    private final MarketStateFingerprintFactory factory = new MarketStateFingerprintFactory();

    @Test
    void quantizesSubPercentNoiseAndIgnoresSampleCount() {
        assertThat(factory.create(statistics(1, .413, .413)))
                .isEqualTo(factory.create(statistics(2, .414, .414)));
    }

    @Test
    void changesForMeaningfulFrequencyOrImportanceDistribution() {
        String baseline = factory.create(statistics(1, .413, .413));
        assertThat(factory.create(statistics(1, .426, .426))).isNotEqualTo(baseline);
        assertThat(factory.create(statistics(1, .413, .20))).isNotEqualTo(baseline);
    }

    private static MarketRequirementStatistics statistics(int sample, double frequency, double requiredFrequency) {
        List<VacancyRequirements> vacancies = java.util.stream.IntStream.range(0, sample)
                .mapToObj(index -> new VacancyRequirements("v" + index,
                        List.of(new MappedRequirement("technology:example", RequirementImportance.REQUIRED))))
                .toList();
        return new MarketRequirementStatistics(ResumeAnalysisProfile.JAVA_BACKEND, sample, sample, sample, 0, true,
                vacancies, List.of(new RequirementStatistics("technology:example", "Example",
                        RequirementType.TECHNOLOGY, sample, sample, 0, 0, frequency,
                        requiredFrequency, 0, 0)));
    }
}
