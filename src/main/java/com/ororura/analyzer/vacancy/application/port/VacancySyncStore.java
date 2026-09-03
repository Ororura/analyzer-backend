package com.ororura.analyzer.vacancy.application.port;

import java.util.List;

import com.ororura.analyzer.vacancy.domain.Vacancy;

public interface VacancySyncStore {

    void markStarted();

    VacancySyncBatchResult persistBatch(List<Vacancy> vacancies);

    void markFinished(boolean successful, String error);
}
