package com.ororura.analyzer.vacancy.requirement;

public record VacancyRequirementInput(String vacancyId, String title, String description) {

    public VacancyRequirementInput {
        if (vacancyId == null || vacancyId.isBlank()) {
            throw new IllegalArgumentException("Vacancy id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Vacancy title must not be blank");
        }
        description = description == null ? "" : description;
    }
}
