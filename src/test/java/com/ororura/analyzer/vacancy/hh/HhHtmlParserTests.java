package com.ororura.analyzer.vacancy.infrastructure.hh;

import com.ororura.analyzer.vacancy.infrastructure.hh.dto.HhDtos.SearchPage;
import com.ororura.analyzer.vacancy.application.port.VacancySourceException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HhHtmlParserTests {

    private final HhHtmlParser parser = new HhHtmlParser(new ObjectMapper());

    @Test
    void parsesEntityEncodedInitialStateUsedByHh() {
        String html = """
                <template id="HH-Lux-InitialState">
                  {&#34;vacancySearchResult&#34;:{&#34;totalResults&#34;:1,
                  &#34;paging&#34;:{&#34;lastPage&#34;:{&#34;page&#34;:0},&#34;next&#34;:{&#34;disabled&#34;:true}},
                  &#34;vacancies&#34;:[{&#34;vacancyId&#34;:&#34;42&#34;,&#34;name&#34;:&#34;Java Developer&#34;,
                  &#34;links&#34;:{&#34;desktop&#34;:&#34;https://hh.ru/vacancy/42&#34;},
                  &#34;snippet&#34;:{&#34;skill&#34;:&#34;Java, Spring Boot&#34;}}]}}
                </template>
                """;

        SearchPage result = parser.parseSearch(html, 0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().id()).isEqualTo("42");
        assertThat(result.items().getFirst().skills()).containsExactly("Java", "Spring Boot");
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void detectsCaptchaMarkup() {
        assertThatThrownBy(() -> parser.parseSearch("<div class=robot-check></div>", 0, 20))
                .isInstanceOfSatisfying(VacancySourceException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("FORBIDDEN");
                    assertThat(exception.getStatus()).isEqualTo(403);
                });
    }
}
