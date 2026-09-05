package com.ororura.analyzer.vacancy.search;

import java.util.List;

import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.persistence.VacancyEntityMapper;
import com.ororura.analyzer.vacancy.persistence.VacancyRepository;
import com.ororura.analyzer.vacancy.search.VacancySpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VacancySearchService {
    private static final String HH_SOURCE = "hh.ru";

    private final VacancyRepository repository;
    private final VacancyEntityMapper mapper;

    public VacancySearchService(VacancyRepository repository, VacancyEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public LocalSearchResult search(VacancySearchCriteria criteria) {
        var page = repository.findAll(VacancySpecifications.matching(criteria),
                PageRequest.of(criteria.page(), criteria.pageSize(), sort(criteria.sort())));
        return new LocalSearchResult(page.getContent().stream().map(mapper::toDomain).toList(),
                page.getTotalPages(), page.getTotalElements(), page.hasNext(), List.of());
    }

    @Transactional(readOnly = true)
    public Vacancy getById(String vacancyId) {
        String externalId = externalId(vacancyId);
        return repository.findBySourceAndExternalId(HH_SOURCE, externalId).filter(entity -> entity.active())
                .map(mapper::toDomain).orElseThrow(() -> new VacancyNotFoundException(vacancyId));
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getByIds(List<String> vacancyIds) {
        List<String> externalIds = vacancyIds.stream().map(VacancySearchService::externalId).distinct().toList();
        var byId = repository.findBySourceAndExternalIdIn(HH_SOURCE, externalIds).stream()
                .filter(entity -> entity.active()).collect(java.util.stream.Collectors.toMap(
                        entity -> entity.externalId(), mapper::toDomain));
        return externalIds.stream().map(id -> {
            Vacancy vacancy = byId.get(id);
            if (vacancy == null) throw new VacancyNotFoundException(id);
            return vacancy;
        }).toList();
    }

    private static String externalId(String id) {
        if (id == null || id.isBlank()) throw new InvalidVacancyQueryException("vacancyId обязателен");
        return id.startsWith("hh-") ? id.substring(3) : id;
    }

    private static Sort sort(VacancySort requested) {
        if (requested == VacancySort.SALARY_DESC) return Sort.by(Sort.Order.desc("salaryFrom").nullsLast(), Sort.Order.asc("id"));
        if (requested == VacancySort.RELEVANCE) return Sort.by(Sort.Order.desc("publishedAt").nullsLast(), Sort.Order.asc("id"));
        return Sort.by(Sort.Order.desc("publishedAt").nullsLast(), Sort.Order.asc("id"));
    }

    public record LocalSearchResult(List<Vacancy> items, int totalPages, long totalElements,
            boolean hasNext, List<String> warnings) {
    }
}
