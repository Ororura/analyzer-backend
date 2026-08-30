package com.ororura.analyzer.vacancy.search;

import java.util.List;

import com.ororura.analyzer.vacancy.model.Pagination;
import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.model.VacancySearchItem;
import com.ororura.analyzer.vacancy.model.VacancySearchResult;
import com.ororura.analyzer.vacancy.cache.VacancyCacheFacade;
import com.ororura.analyzer.vacancy.cache.VacancyMarketVersionService;
import org.springframework.stereotype.Service;

@Service
public class VacancyQueryService {
    private static final int MAX_PAGES = 10;
    private static final int MAX_VACANCIES = 200;

    private final VacancySearchService searchService;
    private final VacancyCriteriaNormalizer criteriaNormalizer;
    private final VacancyCacheFacade cache;
    private final VacancySearchCacheKey cacheKey;
    private final VacancyMarketVersionService marketVersion;

    public VacancyQueryService(VacancySearchService searchService, VacancyCriteriaNormalizer criteriaNormalizer,
            VacancyCacheFacade cache, VacancySearchCacheKey cacheKey, VacancyMarketVersionService marketVersion) {
        this.searchService = searchService;
        this.criteriaNormalizer = criteriaNormalizer;
        this.cache = cache;
        this.cacheKey = cacheKey;
        this.marketVersion = marketVersion;
    }

    public VacancySearchResult search(VacancySearchQuery request) { return search(request.toCriteria()); }

    public VacancySearchResult search(VacancySearchCriteria request) {
        VacancySearchCriteria criteria = criteriaNormalizer.normalize(request);
        String key = cacheKey.create(marketVersion.current(), criteria);
        return cache.search(key, () -> toApi(searchDomainNormalized(criteria)));
    }

    public DomainSearchResult searchDomain(VacancySearchCriteria request) {
        return searchDomainNormalized(criteriaNormalizer.normalize(request));
    }

    public Vacancy getById(String vacancyId) {
        String externalId = vacancyId != null && vacancyId.startsWith("hh-") ? vacancyId.substring(3) : vacancyId;
        return cache.details("hh.ru:" + externalId, () -> searchService.getById(vacancyId));
    }

    public List<Vacancy> getByIds(List<String> vacancyIds) {
        return vacancyIds.stream().map(this::getById).toList();
    }

    private DomainSearchResult searchDomainNormalized(VacancySearchCriteria criteria) {
        VacancySearchService.LocalSearchResult source = searchService.search(criteria);
        return new DomainSearchResult(source.items(), source.totalPages(), source.totalElements(),
                source.hasNext(), source.warnings(), criteria);
    }

    private VacancySearchResult toApi(DomainSearchResult source) {
        VacancySearchCriteria criteria = source.criteria();
        int maxPagesForSize = Math.min(MAX_PAGES, (int) Math.ceil((double) MAX_VACANCIES / criteria.pageSize()));
        int totalPages = Math.min(source.totalPages(), maxPagesForSize);
        boolean hasNext = source.hasNext() && criteria.page() + 1 < maxPagesForSize;
        return new VacancySearchResult(source.items().stream().map(VacancySearchItem::from).toList(),
                criteria.page(), criteria.pageSize(), totalPages, source.totalElements(),
                new Pagination(criteria.page(), criteria.pageSize(), totalPages, hasNext), source.warnings());
    }

    public record DomainSearchResult(List<Vacancy> items, Integer totalPages, Long totalElements,
            boolean hasNext, List<String> warnings, VacancySearchCriteria criteria) {
    }
}
