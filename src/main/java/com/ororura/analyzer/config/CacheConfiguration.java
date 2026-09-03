package com.ororura.analyzer.config;

import java.util.Map;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.application.port.VacancyQueryResult;
import com.ororura.analyzer.market.domain.VacancyMarketData;
import com.ororura.analyzer.market.infrastructure.cache.MarketCacheAdapter;
import com.ororura.analyzer.vacancy.infrastructure.cache.VacancyCacheFacade;
import com.ororura.analyzer.vacancy.infrastructure.cache.VacancyCacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.cache.support.NoOpCacheManager;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VacancyCacheProperties.class)
public class CacheConfiguration {
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    CacheManager vacancyCacheManager(RedisConnectionFactory connectionFactory, VacancyCacheProperties properties,
            ObjectMapper objectMapper) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues().prefixCacheNameWith("vacancy:v2:");
        RedisCacheConfiguration details = defaults.entryTtl(properties.detailsTtl()).serializeValuesWith(
                SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(objectMapper, Vacancy.class)));
        RedisCacheConfiguration search = defaults.entryTtl(properties.searchTtl()).serializeValuesWith(
                SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(objectMapper, VacancyQueryResult.class)));
        RedisCacheConfiguration market = defaults.entryTtl(properties.searchTtl()).serializeValuesWith(
                SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(objectMapper, VacancyMarketData.class)));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults)
                .disableCreateOnMissingCache()
                .withInitialCacheConfigurations(Map.of(
                        VacancyCacheFacade.DETAILS, details,
                        VacancyCacheFacade.SEARCH, search,
                        MarketCacheAdapter.CACHE_NAME, market))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "none")
    CacheManager noOpVacancyCacheManager() {
        return new NoOpCacheManager();
    }
}
