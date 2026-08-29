package com.ororura.analyzer.vacancy;

import java.util.List;

public record VacancySelection(
        SelectionMode mode,
        List<String> vacancyIds,
        VacancySearchCriteria criteria,
        List<String> excludedVacancyIds) {

    public VacancySelection {
        vacancyIds = vacancyIds == null ? List.of() : List.copyOf(vacancyIds);
        excludedVacancyIds = excludedVacancyIds == null ? List.of() : List.copyOf(excludedVacancyIds);
    }
}
