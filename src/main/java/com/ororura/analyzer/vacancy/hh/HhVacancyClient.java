package com.ororura.analyzer.vacancy.hh;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ororura.analyzer.vacancy.VacancySearchQuery;
import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import com.ororura.analyzer.vacancy.hh.HhLocationResolver.ResolvedQuery;
import com.ororura.analyzer.vacancy.hh.dto.HhDtos.SearchPage;
import com.ororura.analyzer.vacancy.hh.dto.HhDtos.SourceVacancy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HhVacancyClient {

    private static final int DETAIL_CONCURRENCY = 4;

    private final RestClient restClient;
    private final HhHtmlParser parser;
    private final HhVacancyMapper mapper;

    public HhVacancyClient(
            @Qualifier(HhConfiguration.REST_CLIENT) RestClient restClient,
            HhHtmlParser parser,
            HhVacancyMapper mapper) {
        this.restClient = restClient;
        this.parser = parser;
        this.mapper = mapper;
    }

    public SearchResult search(VacancySearchQuery query) {
        ResolvedQuery resolved = HhLocationResolver.resolve(query.text(), query.location());
        SearchPage searchPage = searchHh(query, resolved);
        List<SourceVacancy> matchingItems = searchPage.items().stream()
                .filter(item -> HhLocationResolver.matchesFallback(item.location(), resolved.fallbackLocation()))
                .toList();
        List<DetailResult> details = loadDetails(matchingItems);
        return new SearchResult(
                details.stream().map(DetailResult::vacancy).toList(),
                searchPage.totalPages(),
                searchPage.hasNext(),
                details.stream().map(DetailResult::warning).filter(Objects::nonNull).toList());
    }

    private SearchPage searchHh(VacancySearchQuery query, ResolvedQuery resolved) {
        try {
            String html = restClient.get()
                    .uri(HhSearchUriBuilder.build(query, resolved))
                    .retrieve()
                    .body(String.class);
            return parser.parseSearch(html, query.page(), query.perPage());
        } catch (RuntimeException exception) {
            throw HhExceptionMapper.map(exception);
        }
    }

    private List<DetailResult> loadDetails(List<SourceVacancy> vacancies) {
        try (ExecutorService executor = Executors.newFixedThreadPool(DETAIL_CONCURRENCY)) {
            List<Future<DetailResult>> futures = vacancies.stream()
                    .map(vacancy -> executor.submit(() -> loadDetail(vacancy)))
                    .toList();
            return futures.stream().map(HhVacancyClient::await).toList();
        }
    }

    private DetailResult loadDetail(SourceVacancy searchVacancy) {
        try {
            String html = restClient.get()
                    .uri("/vacancy/{id}", searchVacancy.id())
                    .retrieve()
                    .body(String.class);
            SourceVacancy detail = parser.parseDetail(html, searchVacancy.id());
            return new DetailResult(mapper.toDomain(mapper.merge(searchVacancy, detail)), null);
        } catch (RuntimeException exception) {
            VacancySourceException sourceError = HhExceptionMapper.map(exception);
            String warning = "Детали вакансии %s недоступны: %s"
                    .formatted(searchVacancy.id(), HhExceptionMapper.detailReason(sourceError.getCode()));
            return new DetailResult(mapper.toDomain(searchVacancy), warning);
        }
    }

    private static DetailResult await(Future<DetailResult> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw HhExceptionMapper.error("UPSTREAM", "Загрузка вакансий прервана", 502, null, exception);
        } catch (Exception exception) {
            throw HhExceptionMapper.error(
                    "UPSTREAM", "Не удалось загрузить детали вакансии", 502, null, exception);
        }
    }

    public record SearchResult(List<Vacancy> items, Integer totalPages, boolean hasNext, List<String> warnings) {
    }

    private record DetailResult(Vacancy vacancy, String warning) {
    }
}
