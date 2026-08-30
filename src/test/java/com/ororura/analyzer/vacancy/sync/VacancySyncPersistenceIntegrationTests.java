package com.ororura.analyzer.vacancy.sync;

import java.util.List;

import com.ororura.analyzer.vacancy.VacancyServiceTests;
import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import com.ororura.analyzer.vacancy.persistence.VacancyRepository;
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
    void createsLeavesUnchangedUpdatesArchivesAndReactivates() {
        persistence.markStarted();
        Vacancy initial = VacancyServiceTests.vacancy("7", "Acme", "Java", 100_000);
        VacancySyncBatchResult created = persistence.persistBatch(List.of(initial));
        assertThat(created.created()).isOne();
        assertThat(jdbc.queryForObject("select count(*) from vacancy_skills where normalized_skill = 'java'",
                Integer.class)).isOne();

        VacancySyncBatchResult unchanged = persistence.persistBatch(List.of(initial));
        assertThat(unchanged.unchanged()).isOne();
        assertThat(unchanged.marketVersion()).isNull();

        Vacancy changed = VacancyServiceTests.vacancy("7", "Acme", "Spring Boot", 120_000);
        VacancySyncBatchResult updated = persistence.persistBatch(List.of(changed));
        assertThat(updated.updated()).isOne();
        assertThat(updated.marketVersion()).isNotNull();

        persistence.archiveConfirmed("7");
        assertThat(repository.findBySourceAndExternalId("hh.ru", "7").orElseThrow().active()).isFalse();
        persistence.persistBatch(List.of(changed));
        assertThat(repository.findBySourceAndExternalId("hh.ru", "7").orElseThrow().active()).isTrue();
    }
}
