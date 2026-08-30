package com.ororura.analyzer.vacancy.model;

import java.io.Serializable;
import java.util.List;

public record VacancySearchResult(
        List<VacancySearchItem> items,
        int page,
        int pageSize,
        Integer totalPages,
        Long totalElements,
        Pagination pagination,
        List<String> warnings) implements Serializable {

    public VacancySearchResult {
        items = List.copyOf(items);
        warnings = List.copyOf(warnings);
    }
}
