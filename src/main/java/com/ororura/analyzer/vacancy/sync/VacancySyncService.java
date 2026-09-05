package com.ororura.analyzer.vacancy.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ororura.analyzer.vacancy.provider.VacancyProvider;
import com.ororura.analyzer.vacancy.provider.VacancyProviderSearchResult;
import com.ororura.analyzer.vacancy.search.VacancySearchCriteria;
import com.ororura.analyzer.vacancy.cache.VacancyCacheFacade;
import com.ororura.analyzer.vacancy.cache.VacancyMarketVersionService;
import com.ororura.analyzer.vacancy.requirement.generation.MarketProfileRefreshCoordinator;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VacancySyncService {
    private static final Logger log = LoggerFactory.getLogger(VacancySyncService.class);

    private final VacancyProvider provider;
    private final VacancySyncPersistenceService persistence;
    private final VacancySyncProperties properties;
    private final VacancyCacheFacade cache;
    private final VacancyMarketVersionService marketVersion;
    private final MarketProfileRefreshCoordinator marketProfiles;
    private final ResumeAnalysisProfileRegistry profiles;
    private final com.ororura.analyzer.analysis.market.ProfileMarketQuerySource userQueries;

    public VacancySyncService(VacancyProvider provider, VacancySyncPersistenceService persistence,
            VacancySyncProperties properties, VacancyCacheFacade cache, VacancyMarketVersionService marketVersion,
            MarketProfileRefreshCoordinator marketProfiles, ResumeAnalysisProfileRegistry profiles) {
        this(provider,persistence,properties,cache,marketVersion,marketProfiles,profiles,null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public VacancySyncService(VacancyProvider provider, VacancySyncPersistenceService persistence,
            VacancySyncProperties properties, VacancyCacheFacade cache, VacancyMarketVersionService marketVersion,
            MarketProfileRefreshCoordinator marketProfiles, ResumeAnalysisProfileRegistry profiles,
            com.ororura.analyzer.analysis.market.ProfileMarketQuerySource userQueries) {
        this.userQueries=userQueries;
        this.provider = provider;
        this.persistence = persistence;
        this.properties = properties;
        this.cache = cache;
        this.marketVersion = marketVersion;
        this.marketProfiles = marketProfiles;
        this.profiles = profiles;
    }

    //TODO decompose
    public VacancySyncStats synchronize() {
        Instant started = Instant.now();
        int fetched = 0, created = 0, updated = 0, unchanged = 0, failed = 0;
        String lastError = null;
        persistence.markStarted();
        int pageSize = Math.max(1, Math.min(50, properties.pageSize()));
        syncQueries:
        for (VacancySearchCriteria plan : searchPlans()) {
            String query = plan.query();
            for (int page = 0; page < Math.min(10, Math.min(properties.pagesPerCycle(), 200 / pageSize)); page++) {
                VacancyProviderSearchResult result;
                try {
                    result = provider.search(plan.withPage(page, pageSize));
                } catch (RuntimeException exception) {
                    failed++;
                    lastError = exception.getMessage();
                    log.warn("Vacancy sync page failed source=hh query={} page={}", query, page, exception);
                    break syncQueries;
                }
                fetched += result.items().size();
                failed += result.warnings().size();
                List<List<com.ororura.analyzer.vacancy.model.Vacancy>> batches = batches(
                        result.items(), Math.max(1, properties.batchSize()));
                for (var batch : batches) {
                    try {
                        VacancySyncBatchResult persisted = persistence.persistBatch(batch);
                        created += persisted.created();
                        updated += persisted.updated();
                        unchanged += persisted.unchanged();
                        afterCommit(persisted);
                    } catch (RuntimeException exception) {
                        failed += batch.size();
                        lastError = exception.getMessage();
                        log.warn("Vacancy sync batch failed source=hh query={} page={} batchSize={}",
                                query, page, batch.size(), exception);
                    }
                }
                if (!result.hasNext() || result.items().isEmpty()) break;
            }
        }
        boolean successful = lastError == null;
        persistence.markFinished(successful, lastError);
        long duration = Duration.between(started, Instant.now()).toMillis();
        VacancySyncStats stats = new VacancySyncStats(fetched, created, updated, unchanged, 0, failed, duration);
        if (successful && created + updated > 0) {
            marketProfiles.refreshAll();
        }
        log.info("Vacancy sync completed: source=hh fetched={} created={} updated={} unchanged={} archived={} failed={} durationMs={}",
                stats.fetched(), stats.created(), stats.updated(), stats.unchanged(), stats.archived(), stats.failed(),
                stats.durationMillis());
        return stats;
    }

    private void afterCommit(VacancySyncBatchResult result) {
        result.changedExternalIds().forEach(id -> cache.evictDetails("hh.ru:" + id));
        if (result.marketVersion() != null) marketVersion.changed(result.marketVersion());
    }

    private VacancySearchCriteria criteria(String query, int page) {
        String area = properties.area() == null || properties.area().isBlank() ? null : properties.area().trim();
        return new VacancySearchCriteria(query, null, area, null, List.of(), null, null,
                null, null, null, null, List.of(), null, null, page, properties.pageSize(), List.of(), List.of());
    }

    private List<VacancySearchCriteria> searchPlans() {
        var legacy = profiles.all().stream().flatMap(profile -> profile.vacancySearchQueries().stream())
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().map(query -> criteria(query, 0));
        return java.util.stream.Stream.concat(legacy,
                userQueries == null ? java.util.stream.Stream.empty() : userQueries.nextPlans().stream()).distinct().toList();
    }

    private static <T> List<List<T>> batches(List<T> values, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int start = 0; start < values.size(); start += size) {
            result.add(values.subList(start, Math.min(start + size, values.size())));
        }
        return result;
    }
}
