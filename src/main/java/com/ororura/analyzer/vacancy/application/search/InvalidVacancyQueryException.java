package com.ororura.analyzer.vacancy.application.search;

public class InvalidVacancyQueryException extends RuntimeException {

    public InvalidVacancyQueryException(String message) {
        super(message);
    }
}
