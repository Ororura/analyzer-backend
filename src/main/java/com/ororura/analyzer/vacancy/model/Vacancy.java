package com.ororura.analyzer.vacancy.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Vacancy(
        String id,
        String hhId,
        String title,
        String company,
        String companyId,
        String url,
        String location,
        Salary salary,
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
        String normalizedAt) implements Serializable {

    @com.fasterxml.jackson.annotation.JsonProperty(value="vacancyGrade", access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public com.ororura.analyzer.analysis.domain.CandidateGrade vacancyGrade() {
        return new com.ororura.analyzer.analysis.domain.GradePolicy().classify(title).vacancyGrade();
    }

    public Vacancy {
        skills = List.copyOf(skills);
        requirements = List.copyOf(requirements);
        responsibilities = List.copyOf(responsibilities);
    }
}
