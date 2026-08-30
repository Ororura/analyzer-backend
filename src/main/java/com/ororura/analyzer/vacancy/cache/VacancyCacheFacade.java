package com.ororura.analyzer.vacancy.cache;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class VacancyCacheFacade {
    public static final String DETAILS = "details";
    public static final String SEARCH = "search";
    private static final Logger log = LoggerFactory.getLogger(VacancyCacheFacade.class);
    private final CacheManager cacheManager;

    public VacancyCacheFacade(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public <T> T details(String key, Supplier<T> loader) { return load(DETAILS, key, loader); }
    public <T> T search(String key, Supplier<T> loader) { return load(SEARCH, key, loader); }

    public void evictDetails(String key) {
        try {
            Cache cache = cacheManager.getCache(DETAILS);
            if (cache != null) cache.evict(key);
        } catch (RuntimeException exception) {
            log.warn("Vacancy details cache eviction failed key={}", key, exception);
        }
    }

    private <T> T load(String cacheName, String key, Supplier<T> loader) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) return cache.get(key, loader::get);
        } catch (RuntimeException exception) {
            log.warn("Vacancy cache unavailable cache={} key={}; using PostgreSQL", cacheName, key, exception);
        }
        return loader.get();
    }
}
