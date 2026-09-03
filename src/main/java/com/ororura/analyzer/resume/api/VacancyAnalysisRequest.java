package com.ororura.analyzer.resume.api;

import com.ororura.analyzer.vacancy.application.selection.VacancySelection;

public record VacancyAnalysisRequest(
        VacancyAnalysisMode mode,
        String vacancyId,
        VacancySelection selection) {

    public static VacancyAnalysisRequest autoMarket() {
        return new VacancyAnalysisRequest(VacancyAnalysisMode.AUTO_MARKET, null, null);
    }
}
