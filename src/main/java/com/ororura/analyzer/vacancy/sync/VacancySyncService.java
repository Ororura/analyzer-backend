package com.ororura.analyzer.vacancy.sync;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ororura.analyzer.vacancy.VacancyProvider;
import com.ororura.analyzer.vacancy.VacancyProviderSearchResult;
import com.ororura.analyzer.vacancy.VacancySearchCriteria;
import com.ororura.analyzer.vacancy.cache.VacancyCacheFacade;
import com.ororura.analyzer.vacancy.cache.VacancyMarketVersionService;
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

    public VacancySyncService(VacancyProvider provider, VacancySyncPersistenceService persistence,
            VacancySyncProperties properties, VacancyCacheFacade cache, VacancyMarketVersionService marketVersion) {
        this.provider = provider;
        this.persistence = persistence;
        this.properties = properties;
        this.cache = cache;
        this.marketVersion = marketVersion;
    }

    //TODO decompose
    public VacancySyncStats synchronize() {
        Instant started = Instant.now();
        int fetched = 0, created = 0, updated = 0, unchanged = 0, failed = 0;
        String lastError = null;
        persistence.markStarted();
        for (int page = 0; page < properties.pagesPerCycle(); page++) {
            VacancyProviderSearchResult result;
            try {
                result = provider.search(criteria(page));
            } catch (RuntimeException exception) {
                failed++;
                lastError = exception.getMessage();
                log.warn("Vacancy sync page failed source=hh page={}", page, exception);
                break;
            }
            fetched += result.items().size();
            failed += result.warnings().size();
            List<List<com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy>> batches = batches(
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
                    log.warn("Vacancy sync batch failed source=hh page={} batchSize={}", page, batch.size(), exception);
                }
            }
            if (!result.hasNext() || result.items().isEmpty()) break;
        }
        boolean successful = lastError == null;
        persistence.markFinished(successful, lastError);
        long duration = Duration.between(started, Instant.now()).toMillis();
        VacancySyncStats stats = new VacancySyncStats(fetched, created, updated, unchanged, 0, failed, duration);
        log.info("Vacancy sync completed: source=hh fetched={} created={} updated={} unchanged={} archived={} failed={} durationMs={}",
                stats.fetched(), stats.created(), stats.updated(), stats.unchanged(), stats.archived(), stats.failed(),
                stats.durationMillis());
        return stats;
    }

    private void afterCommit(VacancySyncBatchResult result) {
        result.changedExternalIds().forEach(id -> cache.evictDetails("hh.ru:" + id));
        if (result.marketVersion() != null) marketVersion.changed(result.marketVersion());
    }

    private VacancySearchCriteria criteria(int page) {
        String area = properties.area() == null || properties.area().isBlank() ? null : properties.area().trim();
        return new VacancySearchCriteria(properties.query(), null, area, null, List.of(), null, null,
                null, null, null, null, List.of(), null, null, page, properties.pageSize(), List.of(), List.of());
    }

    private static <T> List<List<T>> batches(List<T> values, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int start = 0; start < values.size(); start += size) {
            result.add(values.subList(start, Math.min(start + size, values.size())));
        }
        return result;
    }
}
