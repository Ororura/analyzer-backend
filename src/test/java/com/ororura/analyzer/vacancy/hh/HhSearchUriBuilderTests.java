package com.ororura.analyzer.vacancy.hh;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.ororura.analyzer.vacancy.VacancySearchQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HhSearchUriBuilderTests {

    @Test
    void mapsSearchQueryToHhUri() {
        VacancySearchQuery query = new VacancySearchQuery(
                "Java Backend", 2, 25, List.of("between1And3", "between3And6"), List.of("full"),
                List.of("remote", "fullDay"), 150000, "Москва");
        HhLocationResolver.ResolvedQuery resolved = HhLocationResolver.resolve(query.text(), query.location());

        String decoded = URLDecoder.decode(
                HhSearchUriBuilder.build(query, resolved).getRawQuery(), StandardCharsets.UTF_8);

        assertThat(decoded).contains(
                "page=2",
                "items_on_page=25",
                "enable_snippets=true",
                "ored_clusters=true",
                "experience=between1And3",
                "experience=between3And6",
                "employment=full",
                "work_format=REMOTE",
                "schedule=fullDay",
                "salary=150000",
                "area=1",
                "text=Java Backend");
    }
}
