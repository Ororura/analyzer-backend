package com.ororura.analyzer.vacancy.requirement.generation;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileDefinition;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.requirement.VacancyRequirementPipeline;
import com.ororura.analyzer.vacancy.search.VacancyQueryService;
import com.ororura.analyzer.vacancy.search.VacancySearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketProfileRefreshCoordinator {
    private static final Logger log = LoggerFactory.getLogger(MarketProfileRefreshCoordinator.class);
    private final ResumeAnalysisProfileRegistry profiles;
    private final VacancyQueryService queries;
    private final VacancyRequirementPipeline requirements;
    private final MarketProfileRefreshService refresh;

    public MarketProfileRefreshCoordinator(ResumeAnalysisProfileRegistry profiles, VacancyQueryService queries,
            VacancyRequirementPipeline requirements, MarketProfileRefreshService refresh) {
        this.profiles = profiles; this.queries = queries; this.requirements = requirements; this.refresh = refresh;
    }

    public void refreshAll() {
        for (ResumeAnalysisProfileDefinition profile : profiles.all()) {
            try {
                Map<String, Vacancy> pool = new LinkedHashMap<>();
                for (String query : profile.vacancySearchQueries()) {
                    for (int page = 0; page < 4; page++) {
                        var result = queries.searchDomain(new VacancySearchQuery(query, page, 50,
                                null, null, null, null, null).toCriteria());
                        result.items().forEach(vacancy -> pool.putIfAbsent(vacancy.id(), vacancy));
                        if (!result.hasNext() || result.items().isEmpty()) break;
                    }
                }
                refresh.refresh(requirements.analyze(profile.profile(), pool.values().stream().toList()));
            } catch (RuntimeException exception) {
                log.warn("Market profile refresh failed profile={}; previous profile remains active",
                        profile.profile(), exception);
            }
        }
    }
}
