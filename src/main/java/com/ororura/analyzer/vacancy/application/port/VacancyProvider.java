package com.ororura.analyzer.vacancy.application.port;

import java.util.List;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.application.search.VacancySearchCriteria;

public interface VacancyProvider {

    VacancyProviderSearchResult search(VacancySearchCriteria criteria);

    Vacancy getById(String vacancyId);

    default List<Vacancy> getByIds(List<String> vacancyIds) {
        return vacancyIds.stream().map(this::getById).toList();
    }
}
