package com.ororura.analyzer.vacancy.market;

import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.search.VacancyQueryService;
import com.ororura.analyzer.vacancy.search.VacancySearchQuery;
import com.ororura.analyzer.vacancy.selection.VacancySelectionException;
import com.ororura.analyzer.vacancy.selection.VacancySelectionResolver;
import com.ororura.analyzer.resume.api.VacancyAnalysisMode;
import com.ororura.analyzer.resume.api.VacancyAnalysisRequest;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.LegacyResumeAnalysisProfileDefinition;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import org.springframework.stereotype.Service;

@Service
public class VacancyMarketService {

    private final VacancyQueryService queryService;
    private final VacancyMarketAggregator aggregator;
    private final VacancySelectionResolver selectionResolver;
    private final ResumeAnalysisProfileRegistry profileRegistry;

    public VacancyMarketService(VacancyQueryService queryService, VacancyMarketAggregator aggregator,
            VacancySelectionResolver selectionResolver, ResumeAnalysisProfileRegistry profileRegistry) {
        this.queryService = queryService;
        this.aggregator = aggregator;
        this.selectionResolver = selectionResolver;
        this.profileRegistry = profileRegistry;
    }

    public VacancyMarketData load(String text) {
        return load(ResumeAnalysisProfile.defaultProfile(), text);
    }

    public VacancyMarketData load(ResumeAnalysisProfile profile, String text) {
        LegacyResumeAnalysisProfileDefinition definition = profileRegistry.get(profile);
        try {
            VacancyQueryService.DomainSearchResult result = queryService.searchDomain(
                    new VacancySearchQuery(text, 0, 20, null, null, null, null, null).toCriteria());
            return aggregator.aggregate(definition,
                    result.items().stream().map(MarketVacancy::from).toList(),
                    result.warnings());
        } catch (RuntimeException exception) {
            return aggregator.baseline(definition, "Локальный рынок вакансий пока пуст или недоступен");
        }
    }

    public VacancyMarketData load(ResumeAnalysisProfile profile, VacancyAnalysisRequest request,
            String automaticTargetRole) {
        LegacyResumeAnalysisProfileDefinition definition = profileRegistry.get(profile);
        VacancyAnalysisRequest resolved = request == null ? VacancyAnalysisRequest.autoMarket() : request;
        VacancyAnalysisMode mode = resolved.mode() == null ? VacancyAnalysisMode.AUTO_MARKET : resolved.mode();
        return switch (mode) {
            case AUTO_MARKET -> load(profile, automaticTargetRole);
            case SINGLE_VACANCY -> {
                if (resolved.vacancyId() == null || resolved.vacancyId().isBlank()) {
                    throw new VacancySelectionException("vacancyId обязателен для SINGLE_VACANCY");
                }
                Vacancy vacancy = queryService.getById(resolved.vacancyId());
                yield aggregator.aggregate(definition, java.util.List.of(MarketVacancy.from(vacancy)), java.util.List.of(),
                        "single_vacancy");
            }
            case SELECTED_VACANCIES -> aggregator.aggregate(definition,
                    selectionResolver.resolve(resolved.selection()).stream()
                            .map(MarketVacancy::from).toList(),
                    java.util.List.of(), "selected_vacancies");
        };
    }
}
