package com.ororura.analyzer.vacancy.application.port;

import java.util.function.Supplier;

import com.ororura.analyzer.vacancy.domain.Vacancy;

public interface VacancyQueryCache {

    Vacancy details(String key, Supplier<Vacancy> loader);

    VacancyQueryResult search(String key, Supplier<VacancyQueryResult> loader);

    void evictDetails(String key);
}
