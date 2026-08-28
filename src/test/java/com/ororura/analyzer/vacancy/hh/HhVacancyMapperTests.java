package com.ororura.analyzer.vacancy.hh;

import java.util.List;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Salary;
import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import com.ororura.analyzer.vacancy.hh.dto.HhDtos.SourceVacancy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HhVacancyMapperTests {

    private final HhVacancyMapper mapper = new HhVacancyMapper();

    @Test
    void mapsHhVacancyToDomainContract() {
        SourceVacancy source = vacancy(
                "42", "Java Developer", "Acme", List.of("Java", "Spring Boot"), "Краткое описание");

        Vacancy result = mapper.toDomain(source);

        assertThat(result.id()).isEqualTo("hh-42");
        assertThat(result.hhId()).isEqualTo("42");
        assertThat(result.title()).isEqualTo("Java Developer");
        assertThat(result.company()).isEqualTo("Acme");
        assertThat(result.skills()).containsExactly("Java", "Spring Boot");
        assertThat(result.salary()).isEqualTo(new Salary(180000, 230000, "RUR", true));
        assertThat(result.source()).isEqualTo("hh.ru");
        assertThat(result.normalizedAt()).isNotBlank();
    }

    @Test
    void mergesDetailWithoutDroppingSearchSnippets() {
        SourceVacancy search = vacancy("42", "Java Developer", "Acme", List.of("Java"), "Краткое описание");
        SourceVacancy detail = vacancy("42", null, null, List.of("Java", "Spring Boot"), "Полное описание");

        SourceVacancy merged = mapper.merge(search, detail);

        assertThat(merged.title()).isEqualTo("Java Developer");
        assertThat(merged.description()).isEqualTo("Полное описание");
        assertThat(merged.skills()).containsExactly("Java", "Spring Boot");
        assertThat(merged.requirements()).containsExactly("Java 21");
        assertThat(merged.responsibilities()).containsExactly("Backend development");
    }

    private static SourceVacancy vacancy(
            String id, String title, String company, List<String> skills, String description) {
        return new SourceVacancy(
                id,
                title,
                company,
                company == null ? null : "77",
                "https://hh.ru/vacancy/" + id,
                "Москва",
                company == null ? null : new Salary(180000, 230000, "RUR", true),
                description,
                skills,
                company == null ? List.of() : List.of("Java 21"),
                company == null ? List.of() : List.of("Backend development"),
                company == null ? null : "1–3 года",
                company == null ? null : "Полная занятость",
                null,
                company == null ? null : "Удалённо",
                null);
    }
}
