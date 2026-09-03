package com.ororura.analyzer.vacancy.application.search;

import java.util.List;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.application.port.VacancyCatalog;
import com.ororura.analyzer.vacancy.application.port.VacancyQueryCache;
import com.ororura.analyzer.vacancy.application.port.VacancyQueryResult;
import com.ororura.analyzer.vacancy.application.port.VacancyMarketVersion;
import org.springframework.stereotype.Service;

@Service
public class VacancyQueryService {
    private static final int MAX_PAGES = 10;
    private static final int MAX_VACANCIES = 200;

    private final VacancyCatalog catalog;
    private final VacancyCriteriaNormalizer criteriaNormalizer;
    private final VacancyQueryCache cache;
    private final VacancySearchCacheKey cacheKey;
    private final VacancyMarketVersion marketVersion;

    public VacancyQueryService(VacancyCatalog catalog, VacancyCriteriaNormalizer criteriaNormalizer,
            VacancyQueryCache cache, VacancySearchCacheKey cacheKey, VacancyMarketVersion marketVersion) {
        this.catalog = catalog;
        this.criteriaNormalizer = criteriaNormalizer;
        this.cache = cache;
        this.cacheKey = cacheKey;
        this.marketVersion = marketVersion;
    }

    public VacancyQueryResult search(VacancySearchQuery request) { return search(request.toCriteria()); }

    public VacancyQueryResult search(VacancySearchCriteria request) {
        VacancySearchCriteria criteria = criteriaNormalizer.normalize(request);
        String key = cacheKey.create(marketVersion.current(), criteria);
        return cache.search(key, () -> bounded(searchDomainNormalized(criteria)));
    }

    public DomainSearchResult searchDomain(VacancySearchCriteria request) {
        return searchDomainNormalized(criteriaNormalizer.normalize(request));
    }

    public Vacancy getById(String vacancyId) {
        String externalId = vacancyId != null && vacancyId.startsWith("hh-") ? vacancyId.substring(3) : vacancyId;
        return cache.details("hh.ru:" + externalId, () -> catalog.getById(vacancyId));
    }

    public List<Vacancy> getByIds(List<String> vacancyIds) {
        return vacancyIds.stream().map(this::getById).toList();
    }

    private DomainSearchResult searchDomainNormalized(VacancySearchCriteria criteria) {
        VacancyCatalog.SearchPage source = catalog.search(criteria);
        return new DomainSearchResult(source.items(), source.totalPages(), source.totalElements(),
                source.hasNext(), source.warnings(), criteria);
    }

    private VacancyQueryResult bounded(DomainSearchResult source) {
        VacancySearchCriteria criteria = source.criteria();
        int maxPagesForSize = Math.min(MAX_PAGES, (int) Math.ceil((double) MAX_VACANCIES / criteria.pageSize()));
        int totalPages = Math.min(source.totalPages(), maxPagesForSize);
        boolean hasNext = source.hasNext() && criteria.page() + 1 < maxPagesForSize;
        return new VacancyQueryResult(source.items(), criteria.page(), criteria.pageSize(), totalPages,
                source.totalElements(), hasNext, source.warnings());
    }

    public record DomainSearchResult(List<Vacancy> items, Integer totalPages, Long totalElements,
            boolean hasNext, List<String> warnings, VacancySearchCriteria criteria) {
    }
}
