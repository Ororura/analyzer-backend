package com.ororura.analyzer.vacancy.infrastructure.cache;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ororura.analyzer.vacancy.application.port.VacancyQueryResult;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VacancyCacheTests {

    @Test
    void searchCacheSerializesTypedJsonAndRestoresCurrentDtoClass() {
        var configuration = new VacancyCacheConfiguration();
        var manager = (RedisCacheManager) configuration.vacancyCacheManager(
                mock(RedisConnectionFactory.class),
                new VacancyCacheProperties(Duration.ofMinutes(45), Duration.ofMinutes(5)),
                new ObjectMapper());
        manager.afterPropertiesSet();
        RedisCacheConfiguration search = manager.getCacheConfigurations().get(VacancyCacheFacade.SEARCH);
        var expected = result();

        ByteBuffer serialized = search.getValueSerializationPair().write(expected);
        String json = StandardCharsets.UTF_8.decode(serialized.asReadOnlyBuffer()).toString();
        Object restored = search.getValueSerializationPair().read(serialized.asReadOnlyBuffer());

        assertThat(json).startsWith("{");
        assertThat(restored).isExactlyInstanceOf(VacancyQueryResult.class).isEqualTo(expected);
        assertThat(search.getKeyPrefixFor(VacancyCacheFacade.SEARCH)).isEqualTo("vacancy:v2:search::");
    }

    @Test
    void secondSearchReturnsTypedCacheHitWithoutCallingLoaderAgain() {
        var loads = new AtomicInteger();
        var facade = new VacancyCacheFacade(new ConcurrentMapCacheManager(VacancyCacheFacade.SEARCH));

        VacancyQueryResult first = facade.search("key", () -> {
            loads.incrementAndGet();
            return result();
        });
        VacancyQueryResult second = facade.search("key", () -> {
            loads.incrementAndGet();
            return result();
        });

        assertThat(second).isSameAs(first);
        assertThat(loads).hasValue(1);
    }

    @Test
    void wrongTypedEntryIsReplacedInsteadOfEscapingAsClassCastException() {
        var manager = new ConcurrentMapCacheManager(VacancyCacheFacade.SEARCH);
        manager.getCache(VacancyCacheFacade.SEARCH).put("key", "object from an incompatible class loader");
        var facade = new VacancyCacheFacade(manager);

        VacancyQueryResult loaded = facade.search("key", VacancyCacheTests::result);
        VacancyQueryResult cached = facade.search("key", () -> {
            throw new AssertionError("cache hit must not call the loader");
        });

        assertThat(loaded).isEqualTo(result());
        assertThat(cached).isSameAs(loaded);
    }

    private static VacancyQueryResult result() {
        return new VacancyQueryResult(List.of(), 0, 20, 0, 0L, false, List.of());
    }
}
