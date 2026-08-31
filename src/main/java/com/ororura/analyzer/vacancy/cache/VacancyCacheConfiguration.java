package com.ororura.analyzer.vacancy.cache;

import java.util.Map;

import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.model.VacancySearchResult;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
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
class VacancyCacheConfiguration {
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
                        new JacksonJsonRedisSerializer<>(objectMapper, VacancySearchResult.class)));
        RedisCacheConfiguration market = defaults.entryTtl(properties.searchTtl()).serializeValuesWith(
                SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(objectMapper, VacancyMarketData.class)));
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults)
                .disableCreateOnMissingCache()
                .withInitialCacheConfigurations(Map.of(
                        VacancyCacheFacade.DETAILS, details,
                        VacancyCacheFacade.SEARCH, search,
                        VacancyCacheFacade.MARKET, market))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "none")
    CacheManager noOpVacancyCacheManager() {
        return new NoOpCacheManager();
    }
}
