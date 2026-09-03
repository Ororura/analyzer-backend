package com.ororura.analyzer.vacancy.application.port;

import java.util.List;

import com.ororura.analyzer.vacancy.domain.Vacancy;

public record VacancyQueryResult(
        List<Vacancy> items,
        int page,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean hasNext,
        List<String> warnings) {

    public VacancyQueryResult {
        items = List.copyOf(items);
        warnings = List.copyOf(warnings);
    }
}
