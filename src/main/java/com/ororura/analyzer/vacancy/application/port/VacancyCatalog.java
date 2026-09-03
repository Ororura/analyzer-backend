package com.ororura.analyzer.vacancy.application.port;

import java.util.List;

import com.ororura.analyzer.vacancy.application.search.VacancySearchCriteria;
import com.ororura.analyzer.vacancy.domain.Vacancy;

public interface VacancyCatalog {

    SearchPage search(VacancySearchCriteria criteria);

    Vacancy getById(String vacancyId);

    List<Vacancy> getByIds(List<String> vacancyIds);

    record SearchPage(List<Vacancy> items, int totalPages, long totalElements,
            boolean hasNext, List<String> warnings) {
        public SearchPage {
            items = List.copyOf(items);
            warnings = List.copyOf(warnings);
        }
    }
}
