package com.ororura.analyzer.market.application;

import com.ororura.analyzer.vacancy.application.selection.VacancySelection;

public record VacancyMarketRequest(Mode mode, String vacancyId, VacancySelection selection) {

    public VacancyMarketRequest {
        mode = mode == null ? Mode.AUTO_MARKET : mode;
    }

    public static VacancyMarketRequest autoMarket() {
        return new VacancyMarketRequest(Mode.AUTO_MARKET, null, null);
    }

    public enum Mode { AUTO_MARKET, SINGLE_VACANCY, SELECTED_VACANCIES }
}
