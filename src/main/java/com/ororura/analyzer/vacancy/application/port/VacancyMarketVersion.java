package com.ororura.analyzer.vacancy.application.port;

public interface VacancyMarketVersion {

    long current();

    void changed(long version);
}
