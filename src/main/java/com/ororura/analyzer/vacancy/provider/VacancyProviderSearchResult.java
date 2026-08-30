package com.ororura.analyzer.vacancy.provider;

import java.util.List;

import com.ororura.analyzer.vacancy.model.Vacancy;

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
