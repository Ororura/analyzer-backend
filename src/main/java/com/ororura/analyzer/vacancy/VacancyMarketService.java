package com.ororura.analyzer.vacancy;

import com.ororura.analyzer.vacancy.api.VacancyDtos;
import com.ororura.analyzer.vacancy.market.VacancyMarketAggregator;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import com.ororura.analyzer.resume.api.VacancyAnalysisMode;
import com.ororura.analyzer.resume.api.VacancyAnalysisRequest;
import org.springframework.stereotype.Service;

@Service
public class VacancyMarketService {

    private final VacancyService vacancyService;
    private final VacancyMarketAggregator aggregator;
    private final VacancySelectionResolver selectionResolver;

    public VacancyMarketService(VacancyService vacancyService, VacancyMarketAggregator aggregator,
            VacancySelectionResolver selectionResolver) {
        this.vacancyService = vacancyService;
        this.aggregator = aggregator;
        this.selectionResolver = selectionResolver;
    }

    public VacancyMarketData load(String text) {
        try {
            VacancyService.DomainSearchResult result = vacancyService.searchDomain(
                    new VacancySearchQuery(text, 0, 20, null, null, null, null, null).toCriteria());
            return aggregator.aggregate(
                    result.items().stream().map(VacancyDtos.Vacancy::toMarketVacancy).toList(),
                    result.warnings());
        } catch (RuntimeException exception) {
            return aggregator.baseline("HH.ru временно недоступен");
        }
    }

    public VacancyMarketData load(VacancyAnalysisRequest request, String automaticTargetRole) {
        VacancyAnalysisRequest resolved = request == null ? VacancyAnalysisRequest.autoMarket() : request;
        VacancyAnalysisMode mode = resolved.mode() == null ? VacancyAnalysisMode.AUTO_MARKET : resolved.mode();
        return switch (mode) {
            case AUTO_MARKET -> load(automaticTargetRole);
            case SINGLE_VACANCY -> {
                if (resolved.vacancyId() == null || resolved.vacancyId().isBlank()) {
                    throw new VacancySelectionException("vacancyId обязателен для SINGLE_VACANCY");
                }
                VacancyDtos.Vacancy vacancy = vacancyService.getById(resolved.vacancyId());
                yield aggregator.aggregate(java.util.List.of(vacancy.toMarketVacancy()), java.util.List.of(),
                        "single_vacancy");
            }
            case SELECTED_VACANCIES -> aggregator.aggregate(
                    selectionResolver.resolve(resolved.selection()).stream()
                            .map(VacancyDtos.Vacancy::toMarketVacancy).toList(),
                    java.util.List.of(), "selected_vacancies");
        };
    }
}
