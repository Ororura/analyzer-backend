package com.ororura.analyzer.vacancy;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VacancySelectionResolverTests {

    private final VacancyService service = mock(VacancyService.class);
    private final VacancySelectionResolver resolver = new VacancySelectionResolver(service);

    @Test
    void resolvesSelectedIdsInOrderAndDeduplicates() {
        when(service.getByIds(List.of("1", "2"))).thenReturn(List.of(
                VacancyServiceTests.vacancy("1", "A", "Java", 100_000),
                VacancyServiceTests.vacancy("2", "B", "Spring Boot", 120_000)));

        var result = resolver.resolve(new VacancySelection(SelectionMode.SELECTED,
                List.of("1", "2", "1"), null, List.of()));

        assertThat(result).extracting(vacancy -> vacancy.hhId()).containsExactly("1", "2");
    }

    @Test
    void resolvesAllMatchingAndAppliesExclusions() {
        VacancySearchCriteria criteria = criteria();
        when(service.searchDomain(any())).thenReturn(new VacancyService.DomainSearchResult(List.of(
                VacancyServiceTests.vacancy("1", "A", "Java", 100_000),
                VacancyServiceTests.vacancy("2", "B", "Java", 100_000)),
                1, 2L, false, List.of(), criteria));

        var result = resolver.resolve(new VacancySelection(SelectionMode.ALL_MATCHING,
                List.of(), criteria, List.of("hh-2")));

        assertThat(result).extracting(vacancy -> vacancy.hhId()).containsExactly("1");
    }

    @Test
    void validatesSelectionAndMaximumSize() {
        assertThatThrownBy(() -> resolver.resolve(new VacancySelection(
                SelectionMode.SELECTED, List.of(), null, List.of())))
                .isInstanceOf(VacancySelectionException.class).hasMessageContaining("vacancyIds");
        when(service.searchDomain(any())).thenReturn(new VacancyService.DomainSearchResult(
                List.of(), 10, 30_000L, true, List.of(), criteria()));
        assertThatThrownBy(() -> resolver.resolve(new VacancySelection(
                SelectionMode.ALL_MATCHING, List.of(), criteria(), List.of())))
                .isInstanceOf(VacancySelectionException.class).hasMessageContaining("maximumProcessingSize=200");
    }

    private static VacancySearchCriteria criteria() {
        return new VacancySearchCriteria("Java", null, null, null, List.of(), null, null,
                null, null, null, null, List.of(), null, null, 0, 20, List.of(), List.of());
    }
}
