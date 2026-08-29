package com.ororura.analyzer.vacancy;

import java.util.List;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;

public record VacancyProviderSearchResult(
        List<Vacancy> items,
        Integer totalPages,
        Long totalElements,
        boolean hasNext,
        List<String> warnings) {

    public VacancyProviderSearchResult {
        items = items == null ? List.of() : List.copyOf(items);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
