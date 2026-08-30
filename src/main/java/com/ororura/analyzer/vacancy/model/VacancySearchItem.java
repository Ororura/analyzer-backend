package com.ororura.analyzer.vacancy.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VacancySearchItem(
        String id,
        String hhId,
        String title,
        String company,
        Salary salary,
        String experience,
        String workFormat,
        String location,
        String publishedAt,
        String url,
        List<String> skills,
        List<String> requirements,
        String source) implements Serializable {

    public VacancySearchItem {
        skills = skills == null ? List.of() : List.copyOf(skills);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
    }

    public static VacancySearchItem from(Vacancy vacancy) {
        return new VacancySearchItem(vacancy.id(), vacancy.hhId(), vacancy.title(), vacancy.company(), vacancy.salary(),
                vacancy.experience(), vacancy.workFormat(), vacancy.location(), vacancy.publishedAt(),
                vacancy.url(), vacancy.skills(), vacancy.requirements(), vacancy.source());
    }
}
