package com.ororura.analyzer.vacancy;

import java.util.List;

import com.ororura.analyzer.vacancy.market.MarketVacancy;
import com.ororura.analyzer.vacancy.market.VacancyMarketAggregator;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VacancyMarketAggregatorTests {

    private final VacancyMarketAggregator aggregator = new VacancyMarketAggregator();

    @Test
    void canonicalizesAliasesAndCalculatesVacancyShares() {
        VacancyMarketData result = aggregator.aggregate(List.of(
                vacancy(List.of("Spring", "REST endpoints")),
                vacancy(List.of("Spring Boot"))), List.of());

        assertThat(result.source()).isEqualTo("live");
        assertThat(result.sampleSize()).isEqualTo(2);
        assertThat(result.skillFrequencies().get("Spring Boot")).isEqualTo(1.0);
        assertThat(result.skillFrequencies().get("REST API")).isEqualTo(0.5);
        assertThat(result.experienceRequirements()).containsEntry("1–3 года", 2);
        assertThat(result.workFormats()).containsEntry("Удалённо", 2);
    }

    @Test
    void returnsBaselineWithWarningForEmptySourceData() {
        VacancyMarketData result = aggregator.aggregate(List.of(), List.of("timeout"));

        assertThat(result.source()).isEqualTo("baseline");
        assertThat(result.sampleSize()).isZero();
        assertThat(result.warnings()).containsExactly("timeout");
        assertThat(result.skillFrequencies().get("Java"))
                .isGreaterThan(result.skillFrequencies().get("Kubernetes"));
    }

    private static MarketVacancy vacancy(List<String> skills) {
        return new MarketVacancy(skills, "1–3 года", null, null, "Удалённо");
    }
}
