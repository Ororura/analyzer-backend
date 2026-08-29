package com.ororura.analyzer.vacancy;

import java.util.List;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;

public interface VacancyProvider {

    VacancyProviderSearchResult search(VacancySearchCriteria criteria);

    Vacancy getById(String vacancyId);

    default List<Vacancy> getByIds(List<String> vacancyIds) {
        return vacancyIds.stream().map(this::getById).toList();
    }
}
