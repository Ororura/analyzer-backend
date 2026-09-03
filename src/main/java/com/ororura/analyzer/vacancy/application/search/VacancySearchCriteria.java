package com.ororura.analyzer.vacancy.application.search;

import java.time.LocalDate;
import java.util.List;

public record VacancySearchCriteria(
        String query,
        String title,
        String area,
        String employer,
        List<String> experience,
        String level,
        WorkFormat workFormat,
        Integer salaryFrom,
        Integer salaryTo,
        String currency,
        Boolean salaryOnly,
        List<String> technologies,
        LocalDate publishedFrom,
        VacancySort sort,
        int page,
        int pageSize,
        List<String> employment,
        List<String> schedule) {

    public VacancySearchCriteria {
        experience = experience == null ? List.of() : List.copyOf(experience);
        technologies = technologies == null ? List.of() : List.copyOf(technologies);
        employment = employment == null ? List.of() : List.copyOf(employment);
        schedule = schedule == null ? List.of() : List.copyOf(schedule);
    }

    public VacancySearchCriteria withPage(int newPage, int newPageSize) {
        return new VacancySearchCriteria(query, title, area, employer, experience, level, workFormat,
                salaryFrom, salaryTo, currency, salaryOnly, technologies, publishedFrom, sort,
                newPage, newPageSize, employment, schedule);
    }
}
