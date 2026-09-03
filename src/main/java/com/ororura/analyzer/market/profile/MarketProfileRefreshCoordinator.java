package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileDefinition;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.market.requirement.VacancyRequirementPipeline;
import com.ororura.analyzer.vacancy.application.search.VacancyQueryService;
import com.ororura.analyzer.vacancy.application.search.VacancySearchQuery;
import com.ororura.analyzer.vacancy.application.port.MarketProfileRefresher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketProfileRefreshCoordinator implements MarketProfileRefresher {
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
                    queries.searchDomain(new VacancySearchQuery(query, 0, 200, null, null, null, null, null).toCriteria())
                            .items().forEach(vacancy -> pool.putIfAbsent(vacancy.id(), vacancy));
                }
                refresh.refresh(requirements.analyze(profile.profile(), pool.values().stream().toList()));
            } catch (RuntimeException exception) {
                log.warn("Market profile refresh failed profile={}; previous profile remains active",
                        profile.profile(), exception);
            }
        }
    }
}
