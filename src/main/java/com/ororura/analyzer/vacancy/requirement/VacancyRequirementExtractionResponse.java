package com.ororura.analyzer.vacancy.requirement;

import java.util.List;

record VacancyRequirementExtractionResponse(List<ExtractedVacancyRequirements> vacancies) {

    VacancyRequirementExtractionResponse {
        if (vacancies == null) {
            throw new IllegalArgumentException("Vacancy extraction response must define vacancies");
        }
        vacancies = List.copyOf(vacancies);
    }
}
