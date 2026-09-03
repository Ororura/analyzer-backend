package com.ororura.analyzer.vacancy.application.selection;

import java.util.List;

import com.ororura.analyzer.vacancy.application.search.VacancySearchCriteria;

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
