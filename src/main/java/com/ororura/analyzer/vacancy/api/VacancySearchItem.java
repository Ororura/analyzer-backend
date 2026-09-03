package com.ororura.analyzer.vacancy.api;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VacancySearchItem(
        String id,
        String hhId,
        String title,
        String company,
        SalaryResponse salary,
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

}
