package com.ororura.analyzer.vacancy.application.search;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VacancySearchCacheKeyTests {
    private final VacancySearchCacheKey keys = new VacancySearchCacheKey();

    @Test
    void canonicalCriteriaProduceStableVersionedKey() {
        VacancySearchCriteria first = criteria(List.of("Java", "Spring Boot"), 0);
        VacancySearchCriteria reordered = criteria(List.of(" spring   boot ", "java"), 0);
        assertThat(keys.create(3, first)).isEqualTo(keys.create(3, reordered)).startsWith("v3:");
        assertThat(keys.create(4, first)).isNotEqualTo(keys.create(3, first));
        assertThat(keys.create(3, criteria(List.of("Java"), 1))).isNotEqualTo(keys.create(3, first));
    }

    private static VacancySearchCriteria criteria(List<String> technologies, int page) {
        return new VacancySearchCriteria(" Java ", null, "Moscow", null, List.of(), null, null,
                null, null, null, null, technologies, null, null, page, 20, List.of(), List.of());
    }
}
