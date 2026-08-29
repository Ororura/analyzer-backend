package com.ororura.analyzer.vacancy;

import java.util.List;

public record VacancySearchQuery(
        String text,
        int page,
        int perPage,
        List<String> experience,
        List<String> employment,
        List<String> schedule,
        Integer salary,
        String location) {

    public VacancySearchQuery {
        experience = experience == null ? List.of() : List.copyOf(experience);
        employment = employment == null ? List.of() : List.copyOf(employment);
        schedule = schedule == null ? List.of() : List.copyOf(schedule);
    }

    public VacancySearchCriteria toCriteria() {
        return new VacancySearchCriteria(text, null, location, null, experience, null, null,
                salary, null, null, salary == null ? null : true, List.of(), null, null,
                page, perPage, employment, schedule);
    }
}
