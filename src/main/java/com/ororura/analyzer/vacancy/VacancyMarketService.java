package com.ororura.analyzer.vacancy;

import com.ororura.analyzer.vacancy.api.VacancyDtos;
import com.ororura.analyzer.vacancy.market.VacancyMarketAggregator;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Service;

@Service
public class VacancyMarketService {

    private final VacancyService vacancyService;
    private final VacancyMarketAggregator aggregator;

    public VacancyMarketService(VacancyService vacancyService, VacancyMarketAggregator aggregator) {
        this.vacancyService = vacancyService;
        this.aggregator = aggregator;
    }

    public VacancyMarketData load(String text) {
        try {
            VacancyDtos.VacancySearchResult result = vacancyService.search(
                    new VacancySearchQuery(text, 0, 20, null, null, null, null, null));
            return aggregator.aggregate(
                    result.items().stream().map(VacancyDtos.Vacancy::toMarketVacancy).toList(),
                    result.warnings());
        } catch (RuntimeException exception) {
            return aggregator.baseline("HH.ru временно недоступен");
        }
    }
}
