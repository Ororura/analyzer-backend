package com.ororura.analyzer.vacancy.provider;

import java.util.List;

import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.search.VacancySearchCriteria;

public interface VacancyProvider {

    VacancyProviderSearchResult search(VacancySearchCriteria criteria);

    Vacancy getById(String vacancyId);

    default List<Vacancy> getByIds(List<String> vacancyIds) {
        return vacancyIds.stream().map(this::getById).toList();
    }
}
