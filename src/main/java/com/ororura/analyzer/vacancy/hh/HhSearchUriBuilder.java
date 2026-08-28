package com.ororura.analyzer.vacancy.hh;

import java.net.URI;

import com.ororura.analyzer.vacancy.VacancySearchQuery;
import org.springframework.web.util.UriComponentsBuilder;

final class HhSearchUriBuilder {

    private HhSearchUriBuilder() {
    }

    static URI build(VacancySearchQuery query, HhLocationResolver.ResolvedQuery resolved) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/search/vacancy")
                .queryParam("page", query.page())
                .queryParam("items_on_page", query.perPage())
                .queryParam("enable_snippets", "true")
                .queryParam("ored_clusters", "true");
        query.experience().forEach(value -> uri.queryParam("experience", value));
        query.employment().forEach(value -> uri.queryParam("employment", value));
        query.schedule().forEach(value -> {
            if (value.equals("remote")) uri.queryParam("work_format", "REMOTE");
            else uri.queryParam("schedule", value);
        });
        if (query.salary() != null) uri.queryParam("salary", query.salary());
        if (resolved.area() != null) uri.queryParam("area", resolved.area());
        return uri.queryParam("text", resolved.text()).build().encode().toUri();
    }
}
