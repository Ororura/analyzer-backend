package com.ororura.analyzer.market.application;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.application.port.*;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.application.search.VacancyQueryService;
import com.ororura.analyzer.vacancy.application.search.VacancySearchQuery;
import com.ororura.analyzer.vacancy.application.selection.VacancySelectionException;
import com.ororura.analyzer.vacancy.application.selection.VacancySelectionResolver;
import com.ororura.analyzer.vacancy.application.port.VacancyMarketVersion;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.market.application.port.MarketAnalysisProfileProvider;
import org.springframework.stereotype.Service;
import com.ororura.analyzer.market.requirement.VacancyRequirementPipeline;
import com.ororura.analyzer.market.requirement.ResolvedVacancyRequirements;
import com.ororura.analyzer.market.domain.MarketSkillStatistics;
import com.ororura.analyzer.market.domain.SkillImportance;

@Service
public class VacancyMarketService {

    private final VacancyQueryService queryService;
    private final VacancyMarketAggregator aggregator;
    private final VacancySelectionResolver selectionResolver;
    private final ResumeAnalysisProfileRegistry profileRegistry;
    private final MarketAnalysisProfileProvider marketProfileProvider;
    private final MarketCache cache;
    private final VacancyMarketVersion marketVersion;
    private final VacancyRequirementPipeline requirementPipeline;

    public VacancyMarketService(VacancyQueryService queryService, VacancyMarketAggregator aggregator,
            VacancySelectionResolver selectionResolver, ResumeAnalysisProfileRegistry profileRegistry,
            MarketAnalysisProfileProvider marketProfileProvider, MarketCache cache,
            VacancyMarketVersion marketVersion, VacancyRequirementPipeline requirementPipeline) {
        this.queryService = queryService;
        this.aggregator = aggregator;
        this.selectionResolver = selectionResolver;
        this.profileRegistry = profileRegistry;
        this.marketProfileProvider = marketProfileProvider;
        this.cache = cache;
        this.marketVersion = marketVersion;
        this.requirementPipeline = requirementPipeline;
    }

    public VacancyMarketData load(String text) {
        return load(ResumeAnalysisProfile.defaultProfile(), text);
    }

    public VacancyMarketData load(ResumeAnalysisProfile profile, String text) {
        String key = profile.name() + ":" + marketVersion.current() + ":" + text.trim().toLowerCase(java.util.Locale.ROOT);
        return cache.get(key, () -> loadUncached(profile, text));
    }

    private VacancyMarketData loadUncached(ResumeAnalysisProfile profile, String text) {
        try {
            VacancyQueryService.DomainSearchResult result = queryService.searchDomain(
                    new VacancySearchQuery(text, 0, 20, null, null, null, null, null).toCriteria());
            VacancyMarketData market = aggregator.aggregate(profile,
                    result.items().stream().map(MarketVacancy::from).toList(),
                    result.warnings());
            return versioned(market.sampleSize() == 0 ? fallback(profile) : market);
        } catch (RuntimeException exception) {
            return versioned(fallback(profile));
        }
    }

    public VacancyMarketData load(ResumeAnalysisProfile profile, VacancyMarketRequest request,
            String automaticTargetRole) {
        VacancyMarketRequest resolved = request == null ? VacancyMarketRequest.autoMarket() : request;
        VacancyMarketRequest.Mode mode = resolved.mode();
        return switch (mode) {
            case AUTO_MARKET -> load(profile, automaticTargetRole);
            case SINGLE_VACANCY -> {
                if (resolved.vacancyId() == null || resolved.vacancyId().isBlank()) {
                    throw new VacancySelectionException("vacancyId обязателен для SINGLE_VACANCY");
                }
                String key = "single:" + profile + ":" + marketVersion.current() + ":" + resolved.vacancyId();
                yield cache.get(key, () -> {
                    Vacancy vacancy = queryService.getById(resolved.vacancyId());
                    VacancyMarketData base = aggregator.aggregate(profile, java.util.List.of(MarketVacancy.from(vacancy)),
                            java.util.List.of(), "single_vacancy");
                    try {
                        return versioned(withRequirements(base, requirementPipeline.resolve(profile,
                                java.util.List.of(vacancy))));
                    } catch (RuntimeException exception) {
                        java.util.List<String> warnings = new java.util.ArrayList<>(base.warnings());
                        warnings.add("Не удалось семантически уточнить требования вакансии");
                        return versioned(new VacancyMarketData(base.source(), base.sampleSize(), base.skillFrequencies(),
                                base.experienceRequirements(), base.employmentTypes(), base.workFormats(),
                                base.salaryStatistics(), base.commonRequirements(), base.vacancies(), base.skillStatistics(),
                                base.marketVersion(), base.sufficientSample(), warnings));
                    }
                });
            }
            case SELECTED_VACANCIES -> versioned(aggregator.aggregate(profile,
                    selectionResolver.resolve(resolved.selection()).stream()
                            .map(MarketVacancy::from).toList(),
                    java.util.List.of(), "selected_vacancies"));
        };
    }

    private VacancyMarketData fallback(ResumeAnalysisProfile profile) {
        return aggregator.fallback(marketProfileProvider.getCurrent(profile),
                "Локальный рынок вакансий пока пуст или недоступен");
    }

    private VacancyMarketData versioned(VacancyMarketData value) {
        return new VacancyMarketData(value.source(), value.sampleSize(), value.skillFrequencies(),
                value.experienceRequirements(), value.employmentTypes(), value.workFormats(),
                value.salaryStatistics(), value.commonRequirements(), value.vacancies(), value.skillStatistics(),
                Long.toString(marketVersion.current()), value.sufficientSample(), value.warnings());
    }

    private static VacancyMarketData withRequirements(VacancyMarketData base,
            java.util.List<ResolvedVacancyRequirements> resolved) {
        if (resolved.isEmpty()) return base;
        java.util.Map<String, MarketSkillStatistics> statistics = new java.util.LinkedHashMap<>();
        base.skillStatistics().forEach(value -> statistics.put(value.displayName().toLowerCase(), value));
        resolved.getFirst().requirements().forEach(value -> statistics.put(value.label().toLowerCase(),
                new MarketSkillStatistics(normalized(value.id()), value.label(), 1L, 1L, 1,
                        value.importance() == com.ororura.analyzer.market.requirement.RequirementImportance.REQUIRED ? 1 : 0,
                        value.importance() == com.ororura.analyzer.market.requirement.RequirementImportance.PREFERRED ? 1 : 0,
                        value.importance() == com.ororura.analyzer.market.requirement.RequirementImportance.OPTIONAL ? 1 : 0,
                        value.importance() == com.ororura.analyzer.market.requirement.RequirementImportance.REQUIRED
                                ? SkillImportance.CORE : SkillImportance.MEDIUM)));
        java.util.Map<String, Double> frequencies = new java.util.LinkedHashMap<>(base.skillFrequencies());
        statistics.values().forEach(value -> frequencies.put(value.displayName(), value.frequency()));
        return new VacancyMarketData(base.source(), base.sampleSize(), frequencies, base.experienceRequirements(),
                base.employmentTypes(), base.workFormats(), base.salaryStatistics(), base.commonRequirements(),
                base.vacancies(), java.util.List.copyOf(statistics.values()), base.marketVersion(),
                base.sufficientSample(), base.warnings());
    }

    private static String normalized(String id) {
        int separator = id.indexOf(':');
        return (separator >= 0 ? id.substring(separator + 1) : id).replace('-', '_');
    }
}
