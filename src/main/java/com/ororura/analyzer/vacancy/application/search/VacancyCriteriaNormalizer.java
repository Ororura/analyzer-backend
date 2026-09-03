package com.ororura.analyzer.vacancy.application.search;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class VacancyCriteriaNormalizer {
    private static final int MAX_PAGES = 10;
    private static final int MAX_VACANCIES = 200;
    private static final int MAX_PER_PAGE = 50;

    public VacancySearchCriteria normalize(VacancySearchCriteria query) {
        if (query == null) throw new InvalidVacancyQueryException("criteria обязателен");
        if (query.page() < 0 || query.page() >= MAX_PAGES) {
            throw new InvalidVacancyQueryException("page должен быть от 0 до 9");
        }
        if (query.pageSize() < 1 || query.pageSize() > MAX_PER_PAGE) {
            throw new InvalidVacancyQueryException("pageSize должен быть от 1 до 50");
        }
        if (query.page() * query.pageSize() >= MAX_VACANCIES) {
            throw new InvalidVacancyQueryException("Запрос превышает maxVacancies=200");
        }
        String text = blankToNull(query.query());
        if (text == null) text = "Java Backend Developer";
        if (text.length() > 200) throw new InvalidVacancyQueryException("Поисковый запрос слишком длинный");
        if (query.salaryFrom() != null && query.salaryTo() != null && query.salaryFrom() > query.salaryTo()) {
            throw new InvalidVacancyQueryException("salaryFrom не может быть больше salaryTo");
        }
        return new VacancySearchCriteria(text, blankToNull(query.title()), blankToNull(query.area()),
                blankToNull(query.employer()), query.experience(), blankToNull(query.level()), query.workFormat(),
                query.salaryFrom(), query.salaryTo(), blankToNull(query.currency()), query.salaryOnly(),
                cleanList(query.technologies()), query.publishedFrom(), query.sort(), query.page(), query.pageSize(),
                cleanList(query.employment()), cleanList(query.schedule()));
    }

    private static List<String> cleanList(List<String> values) {
        return values == null ? List.of() : values.stream().map(VacancyCriteriaNormalizer::blankToNull)
                .filter(java.util.Objects::nonNull).distinct().toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
