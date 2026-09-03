package com.ororura.analyzer.vacancy.infrastructure.cache;

import java.util.function.Supplier;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.application.port.VacancyQueryResult;
import com.ororura.analyzer.vacancy.application.port.VacancyQueryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class VacancyCacheFacade implements VacancyQueryCache {
    public static final String DETAILS = "details";
    public static final String SEARCH = "search";
    private static final Logger log = LoggerFactory.getLogger(VacancyCacheFacade.class);
    private final CacheManager cacheManager;

    public VacancyCacheFacade(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Vacancy details(String key, Supplier<Vacancy> loader) {
        return load(DETAILS, key, Vacancy.class, loader);
    }

    public VacancyQueryResult search(String key, Supplier<VacancyQueryResult> loader) {
        return load(SEARCH, key, VacancyQueryResult.class, loader);
    }

    public void evictDetails(String key) {
        try {
            Cache cache = cacheManager.getCache(DETAILS);
            if (cache != null) cache.evict(key);
        } catch (RuntimeException exception) {
            log.warn("Vacancy details cache eviction failed key={}", key, exception);
        }
    }

    private <T> T load(String cacheName, String key, Class<T> valueType, Supplier<T> loader) {
        Cache cache = null;
        try {
            cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                T cached = cache.get(key, valueType);
                if (cached != null) return cached;
            }
        } catch (RuntimeException exception) {
            log.warn("Vacancy cache unavailable cache={} key={}; using PostgreSQL", cacheName, key, exception);
        }
        T loaded = loader.get();
        if (cache != null) {
            try {
                cache.put(key, loaded);
            } catch (RuntimeException exception) {
                log.warn("Vacancy cache write failed cache={} key={}", cacheName, key, exception);
            }
        }
        return loaded;
    }
}
