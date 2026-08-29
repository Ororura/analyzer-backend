package com.ororura.analyzer.vacancy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import org.springframework.stereotype.Service;

@Service
public class VacancySelectionResolver {

    public static final int MAX_SELECTION_SIZE = 200;
    private static final int BATCH_SIZE = 50;

    private final VacancyService vacancyService;

    public VacancySelectionResolver(VacancyService vacancyService) {
        this.vacancyService = vacancyService;
    }

    public List<Vacancy> resolve(VacancySelection selection) {
        validate(selection);
        return switch (selection.mode()) {
            case SELECTED -> resolveIds(selection.vacancyIds());
            case ALL_MATCHING -> resolveAllMatching(selection.criteria(), selection.excludedVacancyIds());
        };
    }

    private List<Vacancy> resolveIds(List<String> ids) {
        List<String> distinct = new ArrayList<>(new LinkedHashSet<>(ids));
        ensureLimit(distinct.size());
        List<Vacancy> result = new ArrayList<>(distinct.size());
        for (int start = 0; start < distinct.size(); start += BATCH_SIZE) {
            result.addAll(vacancyService.getByIds(distinct.subList(start, Math.min(start + BATCH_SIZE, distinct.size()))));
        }
        if (result.isEmpty()) {
            throw new VacancySelectionException("По выбранным критериям не найдено вакансий");
        }
        return List.copyOf(result);
    }

    private List<Vacancy> resolveAllMatching(VacancySearchCriteria criteria, List<String> excludedIds) {
        Set<String> exclusions = Set.copyOf(excludedIds);
        List<Vacancy> result = new ArrayList<>();
        int page = 0;
        while (true) {
            VacancyService.DomainSearchResult batch = vacancyService.searchDomain(criteria.withPage(page, BATCH_SIZE));
            if (batch.totalElements() != null && batch.totalElements() - exclusions.size() > MAX_SELECTION_SIZE) {
                throw tooLarge();
            }
            batch.items().stream()
                    .filter(vacancy -> !exclusions.contains(vacancy.id()) && !exclusions.contains(vacancy.hhId()))
                    .forEach(result::add);
            ensureLimit(result.size());
            if (!batch.hasNext() || batch.items().isEmpty()) break;
            if ((page + 1L) * BATCH_SIZE >= MAX_SELECTION_SIZE) throw tooLarge();
            page++;
        }
        if (result.isEmpty()) {
            throw new VacancySelectionException("По выбранным критериям не найдено вакансий");
        }
        return List.copyOf(result);
    }

    private static void validate(VacancySelection selection) {
        if (selection == null || selection.mode() == null) {
            throw new VacancySelectionException("selection.mode обязателен");
        }
        if (selection.mode() == SelectionMode.SELECTED && selection.vacancyIds().isEmpty()) {
            throw new VacancySelectionException("vacancyIds обязателен и не должен быть пустым для SELECTED");
        }
        if (selection.mode() == SelectionMode.ALL_MATCHING && selection.criteria() == null) {
            throw new VacancySelectionException("criteria обязателен для ALL_MATCHING");
        }
    }

    private static void ensureLimit(long size) {
        if (size > MAX_SELECTION_SIZE) throw tooLarge();
    }

    private static VacancySelectionException tooLarge() {
        return new VacancySelectionException("Выбор превышает maximumProcessingSize=" + MAX_SELECTION_SIZE);
    }
}
