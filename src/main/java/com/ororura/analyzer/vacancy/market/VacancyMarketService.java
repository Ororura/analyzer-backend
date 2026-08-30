package com.ororura.analyzer.vacancy.market;

import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.search.VacancyQueryService;
import com.ororura.analyzer.vacancy.search.VacancySearchQuery;
import com.ororura.analyzer.vacancy.selection.VacancySelectionException;
import com.ororura.analyzer.vacancy.selection.VacancySelectionResolver;
import com.ororura.analyzer.resume.api.VacancyAnalysisMode;
import com.ororura.analyzer.resume.api.VacancyAnalysisRequest;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileProvider;
import org.springframework.stereotype.Service;

@Service
public class VacancyMarketService {

    private final VacancyQueryService queryService;
    private final VacancyMarketAggregator aggregator;
    private final VacancySelectionResolver selectionResolver;
    private final ResumeAnalysisProfileRegistry profileRegistry;
    private final MarketAnalysisProfileProvider marketProfileProvider;

    public VacancyMarketService(VacancyQueryService queryService, VacancyMarketAggregator aggregator,
            VacancySelectionResolver selectionResolver, ResumeAnalysisProfileRegistry profileRegistry,
            MarketAnalysisProfileProvider marketProfileProvider) {
        this.queryService = queryService;
        this.aggregator = aggregator;
        this.selectionResolver = selectionResolver;
        this.profileRegistry = profileRegistry;
        this.marketProfileProvider = marketProfileProvider;
    }

    public VacancyMarketData load(String text) {
        return load(ResumeAnalysisProfile.defaultProfile(), text);
    }

    public VacancyMarketData load(ResumeAnalysisProfile profile, String text) {
        try {
            VacancyQueryService.DomainSearchResult result = queryService.searchDomain(
                    new VacancySearchQuery(text, 0, 20, null, null, null, null, null).toCriteria());
            VacancyMarketData market = aggregator.aggregate(profile,
                    result.items().stream().map(MarketVacancy::from).toList(),
                    result.warnings());
            return market.sampleSize() == 0 ? fallback(profile) : market;
        } catch (RuntimeException exception) {
            return fallback(profile);
        }
    }

    public VacancyMarketData load(ResumeAnalysisProfile profile, VacancyAnalysisRequest request,
            String automaticTargetRole) {
        VacancyAnalysisRequest resolved = request == null ? VacancyAnalysisRequest.autoMarket() : request;
        VacancyAnalysisMode mode = resolved.mode() == null ? VacancyAnalysisMode.AUTO_MARKET : resolved.mode();
        return switch (mode) {
            case AUTO_MARKET -> load(profile, automaticTargetRole);
            case SINGLE_VACANCY -> {
                if (resolved.vacancyId() == null || resolved.vacancyId().isBlank()) {
                    throw new VacancySelectionException("vacancyId обязателен для SINGLE_VACANCY");
                }
                Vacancy vacancy = queryService.getById(resolved.vacancyId());
                yield aggregator.aggregate(profile, java.util.List.of(MarketVacancy.from(vacancy)), java.util.List.of(),
                        "single_vacancy");
            }
            case SELECTED_VACANCIES -> aggregator.aggregate(profile,
                    selectionResolver.resolve(resolved.selection()).stream()
                            .map(MarketVacancy::from).toList(),
                    java.util.List.of(), "selected_vacancies");
        };
    }

    private VacancyMarketData fallback(ResumeAnalysisProfile profile) {
        return aggregator.fallback(marketProfileProvider.getCurrent(profile),
                "Локальный рынок вакансий пока пуст или недоступен");
    }
}
