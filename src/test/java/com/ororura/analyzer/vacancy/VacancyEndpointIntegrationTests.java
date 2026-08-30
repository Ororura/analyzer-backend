package com.ororura.analyzer.vacancy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Salary;
import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import com.ororura.analyzer.vacancy.persistence.VacancyContentHasher;
import com.ororura.analyzer.vacancy.persistence.VacancyEntity;
import com.ororura.analyzer.vacancy.persistence.VacancyNormalizer;
import com.ororura.analyzer.vacancy.persistence.VacancyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VacancyEndpointIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired VacancyRepository repository;
    @Autowired VacancyNormalizer normalizer;
    @Autowired VacancyContentHasher hasher;

    @BeforeEach
    void seedLocalMarket() {
        repository.deleteAll();
        Vacancy vacancy = vacancy("42");
        var content = normalizer.normalize(vacancy);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        VacancyEntity entity = VacancyEntity.create("hh.ru", "42", now);
        entity.replaceContent(content, hasher.hash(content), normalizer.rawPayload(vacancy), now);
        repository.saveAndFlush(entity);
    }

    @Test
    void searchesPostgresAndPreservesResponseContract() throws Exception {
        mockMvc.perform(get("/api/vacancies").param("text", "Java").param("employer", "Acme")
                        .param("workFormat", "REMOTE").param("salaryFrom", "150000")
                        .param("technologies", "Spring Boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("hh-42"))
                .andExpect(jsonPath("$.items[0].hhId").value("42"))
                .andExpect(jsonPath("$.items[0].company").value("Acme"))
                .andExpect(jsonPath("$.items[0].source").value("hh.ru"))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.pageSize").value(20));
    }

    @Test
    void returnsDetailsFromPostgres() throws Exception {
        mockMvc.perform(get("/api/vacancies/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("hh-42"))
                .andExpect(jsonPath("$.description").value("Разрабатывать backend-сервисы"))
                .andExpect(jsonPath("$.skills[1]").value("Spring Boot"));
    }

    @Test
    void mapsMissingLocalVacancyToNotFound() throws Exception {
        mockMvc.perform(get("/api/vacancies/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/vacancies?page=wrong", "/api/vacancies?page=10",
            "/api/vacancies?perPage=0", "/api/vacancies?page=4&perPage=50"})
    void rejectsInvalidParameters(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUERY"));
    }

    private static Vacancy vacancy(String id) {
        return new Vacancy("hh-" + id, id, "Java Backend Developer", "Acme", "77",
                "https://hh.ru/vacancy/" + id, "Москва", new Salary(180_000, 230_000, "RUR", true),
                "Разрабатывать backend-сервисы", List.of("Java", "Spring Boot"),
                List.of("Java и Spring Boot"), List.of("Разрабатывать сервисы"), "1–3 года",
                "Полная занятость", "Удаленная работа", "REMOTE", "hh.ru",
                "2026-08-20T10:00:00+03:00", "2026-08-29T00:00:00Z");
    }
}
