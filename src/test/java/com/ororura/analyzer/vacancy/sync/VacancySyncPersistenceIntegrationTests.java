package com.ororura.analyzer.vacancy.application.sync;

import java.util.List;

import com.ororura.analyzer.vacancy.VacancyQueryServiceTests;
import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyRepository;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancySyncPersistenceService;
import com.ororura.analyzer.vacancy.application.port.VacancySyncBatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VacancySyncPersistenceIntegrationTests {
    @Autowired VacancySyncPersistenceService persistence;
    @Autowired VacancyRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clear() { repository.deleteAll(); }

    @Test
    void createsLeavesUnchangedAndUpdates() {
        persistence.markStarted();
        Vacancy initial = VacancyQueryServiceTests.vacancy("7", "Acme", "Java", 100_000);
        VacancySyncBatchResult created = persistence.persistBatch(List.of(initial));
        assertThat(created.created()).isOne();
        assertThat(jdbc.queryForObject("select count(*) from vacancy_skills where normalized_skill = 'java'",
                Integer.class)).isOne();

        VacancySyncBatchResult unchanged = persistence.persistBatch(List.of(initial));
        assertThat(unchanged.unchanged()).isOne();
        assertThat(unchanged.marketVersion()).isNull();

        Vacancy changed = VacancyQueryServiceTests.vacancy("7", "Acme", "Spring Boot", 120_000);
        VacancySyncBatchResult updated = persistence.persistBatch(List.of(changed));
        assertThat(updated.updated()).isOne();
        assertThat(updated.marketVersion()).isNotNull();

    }
}
