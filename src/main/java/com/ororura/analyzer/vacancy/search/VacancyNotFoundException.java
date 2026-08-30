package com.ororura.analyzer.vacancy.search;

public class VacancyNotFoundException extends RuntimeException {
    public VacancyNotFoundException(String vacancyId) {
        super("Вакансия не найдена: " + vacancyId);
    }
}
