package com.ororura.analyzer.vacancy;

import java.time.LocalDate;
import java.util.List;

import com.ororura.analyzer.vacancy.model.Salary;
import com.ororura.analyzer.vacancy.model.Vacancy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.ororura.analyzer.vacancy.cache.VacancyCacheFacade;
import com.ororura.analyzer.vacancy.cache.VacancyMarketVersionService;
import com.ororura.analyzer.vacancy.search.VacancySearchCacheKey;
import com.ororura.analyzer.vacancy.search.VacancyCriteriaNormalizer;
import com.ororura.analyzer.vacancy.search.VacancyQueryService;
import com.ororura.analyzer.vacancy.search.VacancySearchCriteria;
import com.ororura.analyzer.vacancy.search.VacancySearchService;
import com.ororura.analyzer.vacancy.search.VacancySort;
import com.ororura.analyzer.vacancy.search.WorkFormat;
import org.springframework.cache.support.NoOpCacheManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VacancyQueryServiceTests {

    private final VacancySearchService searchService = mock(VacancySearchService.class);
    private final VacancyMarketVersionService marketVersion = mock(VacancyMarketVersionService.class);
    private final VacancyQueryService service = new VacancyQueryService(searchService, new VacancyCriteriaNormalizer(),
            new VacancyCacheFacade(new NoOpCacheManager()), new VacancySearchCacheKey(), marketVersion);

    @Test
    void appliesBackendFiltersAndReturnsLightweightPagination() {
        VacancySearchCriteria criteria = criteria(List.of("spring-boot"));
        when(searchService.search(org.mockito.ArgumentMatchers.any())).thenReturn(new VacancySearchService.LocalSearchResult(
                List.of(vacancy("1", "Acme", "Spring Boot", 150_000)), 3, 60L, true, List.of()));

        var result = service.search(criteria);

        assertThat(result.items()).extracting(item -> item.id()).containsExactly("hh-1");
        assertThat(result.items().getFirst().workFormat()).isEqualTo("REMOTE");
        assertThat(result.items().getFirst().getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName).doesNotContain("description");
        assertThat(result.page()).isZero();
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(60);
    }

    @Test
    void delegatesDetailsByStableId() {
        Vacancy expected = vacancy("42", "Acme", "Java", 180_000);
        when(searchService.getById("hh-42")).thenReturn(expected);
        assertThat(service.getById("hh-42")).isSameAs(expected);
    }

    private static VacancySearchCriteria criteria(List<String> technologies) {
        return new VacancySearchCriteria("Java", null, null, "Acme", List.of(), "JUNIOR",
                WorkFormat.REMOTE, 100_000, 200_000, "RUR", true, technologies,
                LocalDate.of(2026, 1, 1), VacancySort.DATE, 0, 20, List.of(), List.of());
    }

    public static Vacancy vacancy(String id, String company, String skill, int salary) {
        return new Vacancy("hh-" + id, id, "Java Developer", company, "77", "https://hh.ru/vacancy/" + id,
                "Москва", new Salary(salary, salary + 20_000, "RUR", true),
                "Develop with " + skill, List.of(skill), List.of("Java required"), List.of("Build services"),
                "1–3 года", "Полная занятость", null, "REMOTE", "hh.ru",
                "2026-08-20T10:00:00+03:00", "2026-08-29T00:00:00Z");
    }
}
