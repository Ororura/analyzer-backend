package com.ororura.analyzer.vacancy;

import java.util.List;

import com.ororura.analyzer.vacancy.market.MarketVacancy;
import com.ororura.analyzer.vacancy.market.VacancyMarketAggregator;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import com.ororura.analyzer.vacancy.model.Salary;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;
import com.ororura.analyzer.resume.ai.profile.ReactFrontendAnalysisProfile;
import com.ororura.analyzer.resume.ai.DefaultAnalysisTechnologyCatalog;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.domain.LegacyTechnologyTaxonomy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VacancyMarketAggregatorTests {

    private final VacancyMarketAggregator aggregator = new VacancyMarketAggregator(
            new LegacyTechnologyTaxonomy(), new DefaultAnalysisTechnologyCatalog());
    private final ResumeAnalysisProfile profile = ResumeAnalysisProfile.JAVA_BACKEND;

    @Test
    void canonicalizesAliasesAndCalculatesVacancyShares() {
        VacancyMarketData result = aggregator.aggregate(profile, List.of(
                vacancy(List.of("Spring", "REST endpoints")),
                vacancy(List.of("Spring Boot"))), List.of());

        assertThat(result.source()).isEqualTo("live");
        assertThat(result.sampleSize()).isEqualTo(2);
        assertThat(result.skillFrequencies()).containsEntry("Spring", 0.5).containsEntry("Spring Boot", 0.5);
        assertThat(result.skillFrequencies().get("REST API")).isEqualTo(0.5);
        assertThat(result.experienceRequirements()).containsEntry("1–3 года", 2);
        assertThat(result.workFormats()).containsEntry("Удалённо", 2);
    }

    @Test
    void returnsEmptyMarketWithWarningForEmptySourceData() {
        VacancyMarketData result = aggregator.aggregate(profile, List.of(), List.of("timeout"));

        assertThat(result.source()).isEqualTo("empty");
        assertThat(result.sampleSize()).isZero();
        assertThat(result.warnings()).containsExactly("timeout");
        assertThat(result.skillFrequencies()).isEmpty();
    }

    @Test
    void doesNotReuseJavaBaselineForReactProfile() {
        VacancyMarketData result = aggregator.aggregate(ResumeAnalysisProfile.REACT_FRONTEND, List.of(), List.of());

        assertThat(result.skillFrequencies()).isEmpty();
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    void aggregatesSalaryRequirementsAndKeepsFullContextOnlyForSingleVacancy() {
        MarketVacancy vacancy = new MarketVacancy("hh-1", "Java Developer", List.of("Java"),
                List.of("Spring Boot", "SQL"), List.of("Build APIs"), "Untrusted vacancy text",
                new Salary(120_000, 180_000, "RUR", true), "1–3 года", "Полная занятость", null, "REMOTE");

        VacancyMarketData single = aggregator.aggregate(profile, List.of(vacancy), List.of(), "single_vacancy");
        VacancyMarketData multiple = aggregator.aggregate(profile, List.of(vacancy, vacancy), List.of(), "selected_vacancies");

        assertThat(single.salaryStatistics().minimum()).isEqualTo(120_000);
        assertThat(single.salaryStatistics().maximum()).isEqualTo(180_000);
        assertThat(single.commonRequirements()).contains("Spring Boot", "SQL");
        assertThat(single.skillFrequencies()).containsExactlyEntriesOf(java.util.Map.of("Java", 1.0));
        assertThat(single.vacancies()).singleElement().satisfies(context ->
                assertThat(context.description()).isEqualTo("Untrusted vacancy text"));
        assertThat(multiple.vacancies()).isEmpty();
    }

    private static MarketVacancy vacancy(List<String> skills) {
        return new MarketVacancy(skills, "1–3 года", null, null, "Удалённо");
    }
}
