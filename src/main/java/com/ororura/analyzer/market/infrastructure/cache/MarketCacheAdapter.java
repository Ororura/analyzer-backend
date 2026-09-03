package com.ororura.analyzer.market.infrastructure.cache;

import java.util.function.Supplier;

import com.ororura.analyzer.market.application.port.MarketCache;
import com.ororura.analyzer.market.domain.VacancyMarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class MarketCacheAdapter implements MarketCache {

    public static final String CACHE_NAME = "market";
    private static final Logger log = LoggerFactory.getLogger(MarketCacheAdapter.class);
    private final CacheManager cacheManager;

    public MarketCacheAdapter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public VacancyMarketData get(String key, Supplier<VacancyMarketData> loader) {
        Cache cache = null;
        try {
            cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                VacancyMarketData cached = cache.get(key, VacancyMarketData.class);
                if (cached != null) {
                    return cached;
                }
            }
        } catch (RuntimeException exception) {
            log.warn("Market cache unavailable key={}; loading current snapshot", key, exception);
        }

        VacancyMarketData loaded = loader.get();
        if (cache != null) {
            try {
                cache.put(key, loaded);
            } catch (RuntimeException exception) {
                log.warn("Market cache write failed key={}", key, exception);
            }
        }
        return loaded;
    }
}
