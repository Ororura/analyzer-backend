package com.ororura.analyzer.vacancy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyContentHasher;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyEntity;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyNormalizer;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VacancyMarketEndpointIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired VacancyRepository repository;
    @Autowired VacancyNormalizer normalizer;
    @Autowired VacancyContentHasher hasher;

    @BeforeEach
    void seedLocalMarket() {
        repository.deleteAll();
        Vacancy vacancy = VacancyQueryServiceTests.vacancy("1", "Acme", "Java", 150_000);
        vacancy = new Vacancy(vacancy.id(), vacancy.hhId(), "Java Backend Developer", vacancy.company(),
                vacancy.companyId(), vacancy.url(), vacancy.location(), vacancy.salary(), vacancy.description(),
                List.of("Java", "Spring Boot"), vacancy.requirements(), vacancy.responsibilities(), vacancy.experience(),
                vacancy.employment(), vacancy.schedule(), vacancy.workFormat(), vacancy.source(), vacancy.publishedAt(),
                vacancy.normalizedAt());
        var content = normalizer.normalize(vacancy);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        VacancyEntity entity = VacancyEntity.create("hh.ru", "1", now);
        entity.replaceContent(content, hasher.hash(content), normalizer.rawPayload(vacancy), now);
        repository.saveAndFlush(entity);
    }

    @Test
    void returnsMarketAggregatedFromLocalVacancies() throws Exception {
        mockMvc.perform(get("/api/vacancy-market").param("text", "Java Backend Developer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("live"))
                .andExpect(jsonPath("$.sampleSize").value(1))
                .andExpect(jsonPath("$.skillFrequencies.Java").value(1.0))
                .andExpect(jsonPath("$.skillFrequencies['Spring Boot']").value(1.0));
    }
}
