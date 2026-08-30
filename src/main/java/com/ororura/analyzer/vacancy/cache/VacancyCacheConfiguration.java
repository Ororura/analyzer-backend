package com.ororura.analyzer.vacancy.cache;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.cache.support.NoOpCacheManager;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VacancyCacheProperties.class)
class VacancyCacheConfiguration {
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    CacheManager vacancyCacheManager(RedisConnectionFactory connectionFactory, VacancyCacheProperties properties) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues().prefixCacheNameWith("vacancy:");
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        VacancyCacheFacade.DETAILS, defaults.entryTtl(properties.detailsTtl()),
                        VacancyCacheFacade.SEARCH, defaults.entryTtl(properties.searchTtl())))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "none")
    CacheManager noOpVacancyCacheManager() {
        return new NoOpCacheManager();
    }
}
