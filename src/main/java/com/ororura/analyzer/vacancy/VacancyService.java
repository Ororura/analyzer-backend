package com.ororura.analyzer.vacancy;

import com.ororura.analyzer.vacancy.hh.HhVacancyClient;
import org.springframework.stereotype.Service;

import static com.ororura.analyzer.vacancy.api.VacancyDtos.*;

@Service
public class VacancyService {

    private static final int MAX_PAGES = 10;
    private static final int MAX_VACANCIES = 200;
    private static final int MAX_PER_PAGE = 50;

    private final HhVacancyClient client;

    public VacancyService(HhVacancyClient client) {
        this.client = client;
    }

    public VacancySearchResult search(VacancySearchQuery request) {
        VacancySearchQuery query = normalize(request);
        HhVacancyClient.SearchResult source = client.search(query);
        int maxPagesForSize = Math.min(MAX_PAGES, (int) Math.ceil((double) MAX_VACANCIES / query.perPage()));
        Integer totalPages = source.totalPages() == null
                ? null
                : Math.min(source.totalPages(), maxPagesForSize);
        boolean hasNext = source.hasNext() && query.page() + 1 < maxPagesForSize;
        return new VacancySearchResult(
                source.items(),
                new Pagination(query.page(), query.perPage(), totalPages, hasNext),
                source.warnings());
    }

    private VacancySearchQuery normalize(VacancySearchQuery query) {
        if (query.page() < 0 || query.page() >= MAX_PAGES) {
            throw new InvalidVacancyQueryException("page должен быть от 0 до 9");
        }
        if (query.perPage() < 1 || query.perPage() > MAX_PER_PAGE) {
            throw new InvalidVacancyQueryException("perPage должен быть от 1 до 50");
        }
        if (query.page() * query.perPage() >= MAX_VACANCIES) {
            throw new InvalidVacancyQueryException("Запрос превышает maxVacancies=200");
        }
        String text = query.text() == null || query.text().trim().isEmpty()
                ? "Java Backend Developer"
                : query.text().trim();
        if (text.length() > 200) {
            throw new InvalidVacancyQueryException("Поисковый запрос слишком длинный");
        }
        String location = query.location() == null || query.location().trim().isEmpty()
                ? null
                : query.location().trim();
        return new VacancySearchQuery(text, query.page(), query.perPage(), query.experience(), query.employment(),
                query.schedule(), query.salary(), location);
    }
}
