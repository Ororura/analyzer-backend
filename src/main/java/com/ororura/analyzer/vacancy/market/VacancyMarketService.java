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
import com.ororura.analyzer.vacancy.cache.VacancyCacheFacade;
import com.ororura.analyzer.vacancy.cache.VacancyMarketVersionService;
import com.ororura.analyzer.vacancy.requirement.VacancyRequirementPipeline;
import com.ororura.analyzer.vacancy.requirement.ResolvedVacancyRequirements;
import com.ororura.analyzer.resume.market.MarketSkillStatistics;
import com.ororura.analyzer.resume.market.SkillImportance;

@Service
public class VacancyMarketService {

    private final VacancyQueryService queryService;
    private final VacancyMarketAggregator aggregator;
    private final VacancySelectionResolver selectionResolver;
    private final ResumeAnalysisProfileRegistry profileRegistry;
    private final MarketAnalysisProfileProvider marketProfileProvider;
    private final VacancyCacheFacade cache;
    private final VacancyMarketVersionService marketVersion;
    private final VacancyRequirementPipeline requirementPipeline;

    public VacancyMarketService(VacancyQueryService queryService, VacancyMarketAggregator aggregator,
            VacancySelectionResolver selectionResolver, ResumeAnalysisProfileRegistry profileRegistry,
            MarketAnalysisProfileProvider marketProfileProvider, VacancyCacheFacade cache,
            VacancyMarketVersionService marketVersion, VacancyRequirementPipeline requirementPipeline) {
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
        return cache.market(key, () -> loadUncached(profile, text));
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
                String key = "single:" + profile + ":" + marketVersion.current() + ":" + resolved.vacancyId();
                yield cache.market(key, () -> {
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
                        value.importance() == com.ororura.analyzer.vacancy.requirement.RequirementImportance.REQUIRED ? 1 : 0,
                        value.importance() == com.ororura.analyzer.vacancy.requirement.RequirementImportance.PREFERRED ? 1 : 0,
                        value.importance() == com.ororura.analyzer.vacancy.requirement.RequirementImportance.OPTIONAL ? 1 : 0,
                        value.importance() == com.ororura.analyzer.vacancy.requirement.RequirementImportance.REQUIRED
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
