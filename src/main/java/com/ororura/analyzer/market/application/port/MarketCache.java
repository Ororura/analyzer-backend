package com.ororura.analyzer.market.application.port;

import java.util.function.Supplier;

import com.ororura.analyzer.market.domain.VacancyMarketData;

public interface MarketCache {

    VacancyMarketData get(String key, Supplier<VacancyMarketData> loader);
}
