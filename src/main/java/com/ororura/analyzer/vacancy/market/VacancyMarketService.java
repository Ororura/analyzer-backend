package com.ororura.analyzer.vacancy.market;

import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.search.VacancyQueryService;
import com.ororura.analyzer.vacancy.search.VacancySearchQuery;
import com.ororura.analyzer.vacancy.selection.VacancySelectionException;
import com.ororura.analyzer.vacancy.selection.VacancySelectionResolver;
import com.ororura.analyzer.resume.api.VacancyAnalysisMode;
import com.ororura.analyzer.resume.api.VacancyAnalysisRequest;
import org.springframework.stereotype.Service;

@Service
public class VacancyMarketService {

    private final VacancyQueryService queryService;
    private final VacancyMarketAggregator aggregator;
    private final VacancySelectionResolver selectionResolver;

    public VacancyMarketService(VacancyQueryService queryService, VacancyMarketAggregator aggregator,
            VacancySelectionResolver selectionResolver) {
        this.queryService = queryService;
        this.aggregator = aggregator;
        this.selectionResolver = selectionResolver;
    }

    public VacancyMarketData load(String text) {
        try {
            VacancyQueryService.DomainSearchResult result = queryService.searchDomain(
                    new VacancySearchQuery(text, 0, 20, null, null, null, null, null).toCriteria());
            return aggregator.aggregate(
                    result.items().stream().map(MarketVacancy::from).toList(),
                    result.warnings());
        } catch (RuntimeException exception) {
            return aggregator.baseline("Локальный рынок вакансий пока пуст или недоступен");
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
                Vacancy vacancy = queryService.getById(resolved.vacancyId());
                yield aggregator.aggregate(java.util.List.of(MarketVacancy.from(vacancy)), java.util.List.of(),
                        "single_vacancy");
            }
            case SELECTED_VACANCIES -> aggregator.aggregate(
                    selectionResolver.resolve(resolved.selection()).stream()
                            .map(MarketVacancy::from).toList(),
                    java.util.List.of(), "selected_vacancies");
        };
    }
}
