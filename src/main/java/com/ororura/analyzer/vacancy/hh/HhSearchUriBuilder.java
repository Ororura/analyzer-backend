package com.ororura.analyzer.vacancy.hh;

import java.net.URI;

import com.ororura.analyzer.vacancy.VacancySearchCriteria;
import com.ororura.analyzer.vacancy.VacancySearchQuery;
import org.springframework.web.util.UriComponentsBuilder;

final class HhSearchUriBuilder {

    private HhSearchUriBuilder() {
    }

    static URI build(VacancySearchQuery query, HhLocationResolver.ResolvedQuery resolved) {
        return build(query.toCriteria(), resolved);
    }

    static URI build(VacancySearchCriteria query, HhLocationResolver.ResolvedQuery resolved) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/search/vacancy")
                .queryParam("page", query.page())
                .queryParam("items_on_page", query.pageSize())
                .queryParam("enable_snippets", "true")
                .queryParam("ored_clusters", "true");
        query.experience().forEach(value -> uri.queryParam("experience", value));
        query.employment().forEach(value -> uri.queryParam("employment", value));
        query.schedule().forEach(value -> {
            if (value.equals("remote")) uri.queryParam("work_format", "REMOTE");
            else uri.queryParam("schedule", value);
        });
        if (query.salaryFrom() != null) uri.queryParam("salary", query.salaryFrom());
        if (Boolean.TRUE.equals(query.salaryOnly())) uri.queryParam("only_with_salary", "true");
        if (query.currency() != null) uri.queryParam("currency", query.currency());
        if (query.employer() != null && query.employer().matches("\\d+")) {
            uri.queryParam("employer_id", query.employer());
        }
        if (query.publishedFrom() != null) uri.queryParam("date_from", query.publishedFrom());
        if (query.sort() != null) {
            uri.queryParam("order_by", switch (query.sort()) {
                case DATE -> "publication_time";
                case SALARY_DESC -> "salary_desc";
                case RELEVANCE -> "relevance";
            });
        }
        if (query.workFormat() != null) {
            uri.queryParam("work_format", switch (query.workFormat()) {
                case REMOTE -> "REMOTE";
                case OFFICE -> "ON_SITE";
                case HYBRID -> "HYBRID";
            });
        }
        if (resolved.area() != null) uri.queryParam("area", resolved.area());
        return uri.queryParam("text", resolved.text()).build().encode().toUri();
    }
}
