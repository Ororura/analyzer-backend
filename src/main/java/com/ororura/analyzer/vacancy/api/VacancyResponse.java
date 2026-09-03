package com.ororura.analyzer.vacancy.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Vacancy")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VacancyResponse(
        String id,
        String hhId,
        String title,
        String company,
        String companyId,
        String url,
        String location,
        SalaryResponse salary,
        String description,
        List<String> skills,
        List<String> requirements,
        List<String> responsibilities,
        String experience,
        String employment,
        String schedule,
        String workFormat,
        String source,
        String publishedAt,
        String normalizedAt) {

    public VacancyResponse {
        skills = List.copyOf(skills);
        requirements = List.copyOf(requirements);
        responsibilities = List.copyOf(responsibilities);
    }
}
