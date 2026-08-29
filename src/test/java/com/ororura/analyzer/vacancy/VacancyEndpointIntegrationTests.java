package com.ororura.analyzer.vacancy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VacancyEndpointIntegrationTests {

    private static final HttpServer HH_SERVER = startHhServer();
    private static final List<String> SEARCH_QUERIES = new CopyOnWriteArrayList<>();
    private static final List<String> SEARCH_HEADERS = new CopyOnWriteArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void hhApiProperties(DynamicPropertyRegistry registry) {
        registry.add("app.hh.base-url", () -> "http://localhost:" + HH_SERVER.getAddress().getPort());
    }

    @BeforeEach
    void clearRequests() {
        SEARCH_QUERIES.clear();
        SEARCH_HEADERS.clear();
    }

    @AfterAll
    static void stopHhServer() {
        HH_SERVER.stop(0);
    }

    @Test
    void searchesVacanciesAndPreservesResponseContract() throws Exception {
        mockMvc.perform(get("/api/vacancies")
                        .param("text", "Java")
                        .param("experience", "between1And3", "between3And6")
                        .param("employment", "full")
                        .param("schedule", "remote")
                        .param("salary", "150000")
                        .param("location", "Москва"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("hh-42"))
                .andExpect(jsonPath("$.items[0].hhId").value("42"))
                .andExpect(jsonPath("$.items[0].title").value("Java Developer"))
                .andExpect(jsonPath("$.items[0].company").value("Acme"))
                .andExpect(jsonPath("$.items[0].source").value("hh.ru"))
                .andExpect(jsonPath("$.items[0].skills[0]").value("Java"))
                .andExpect(jsonPath("$.items[0].requirements[0]").value("Java и Spring Boot"))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.pageSize").value(20))
                .andExpect(jsonPath("$.pagination.totalPages").value(3))
                .andExpect(jsonPath("$.pagination.hasNext").value(true))
                .andExpect(jsonPath("$.warnings").isEmpty());

        String query = SEARCH_QUERIES.getFirst();
        assertThat(query).contains("text=Java", "page=0", "items_on_page=20", "experience=between1And3",
                "experience=between3And6", "employment=full", "work_format=REMOTE", "salary=150000", "area=1");
        assertThat(query).doesNotContain("host=");
        assertThat(SEARCH_HEADERS.getFirst())
                .contains("text/html", "ru-RU", "pdf-analyzer/1.0 (public vacancy reader)");
    }

    @Test
    void usesFastifyCompatibleDefaults() throws Exception {
        mockMvc.perform(get("/api/vacancies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.pageSize").value(20));

        assertThat(SEARCH_QUERIES.getFirst())
                .contains("text=Java Backend Developer", "page=0", "items_on_page=20");
    }

    @Test
    void supportsPagination() throws Exception {
        mockMvc.perform(get("/api/vacancies").param("page", "1").param("perPage", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.pageSize").value(5))
                .andExpect(jsonPath("$.pagination.totalPages").value(3))
                .andExpect(jsonPath("$.pagination.hasNext").value(true));

        assertThat(SEARCH_QUERIES.getFirst()).contains("page=1", "items_on_page=5");
    }

    @Test
    void supportsExtendedProviderAndBackendFilters() throws Exception {
        mockMvc.perform(get("/api/vacancies")
                        .param("query", "Java")
                        .param("employer", "Acme")
                        .param("workFormat", "REMOTE")
                        .param("salaryFrom", "100000")
                        .param("salaryTo", "250000")
                        .param("currency", "RUR")
                        .param("salaryOnly", "true")
                        .param("technologies", "spring-boot")
                        .param("publishedFrom", "2026-08-01")
                        .param("sort", "DATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].company").value("Acme"))
                .andExpect(jsonPath("$.items[0].workFormat").value("REMOTE"))
                .andExpect(jsonPath("$.totalElements").value(60));

        assertThat(SEARCH_QUERIES.getFirst()).contains("only_with_salary=true", "currency=RUR",
                "date_from=2026-08-01", "order_by=publication_time", "work_format=REMOTE",
                "text=Java spring-boot");
    }

    @Test
    void returnsFullVacancyDetails() throws Exception {
        mockMvc.perform(get("/api/vacancies/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("hh-42"))
                .andExpect(jsonPath("$.description").value("Разрабатывать backend-сервисы"))
                .andExpect(jsonPath("$.skills[1]").value("Spring Boot"))
                .andExpect(jsonPath("$.workFormat").value("REMOTE"))
                .andExpect(jsonPath("$.source").value("hh.ru"));
    }

    @Test
    void mapsMissingVacancyDetailsToNotFound() throws Exception {
        mockMvc.perform(get("/api/vacancies/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/vacancies?page=wrong",
            "/api/vacancies?page=10",
            "/api/vacancies?perPage=0",
            "/api/vacancies?page=4&perPage=50"
    })
    void rejectsInvalidParameters(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUERY"));
        assertThat(SEARCH_QUERIES).isEmpty();
    }

    @Test
    void mapsSearchUpstreamErrorAndRetryAfter() throws Exception {
        mockMvc.perform(get("/api/vacancies").param("text", "upstream"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("HH.ru временно ограничил запросы"));
    }

    @Test
    void preservesRealForbiddenErrorInsteadOfMaskingIt() throws Exception {
        mockMvc.perform(get("/api/vacancies").param("text", "forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("HH.ru отклонил запрос"));
    }

    @Test
    void keepsSearchItemAndAddsWarningWhenDetailFails() throws Exception {
        mockMvc.perform(get("/api/vacancies").param("text", "detail-fallback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("hh-missing"))
                .andExpect(jsonPath("$.warnings[0]").value(
                        "Детали вакансии missing недоступны: страница не найдена"));
    }

    private static HttpServer startHhServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", VacancyEndpointIntegrationTests::respond);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = decode(exchange.getRequestURI().getRawQuery());
        if (path.equals("/search/vacancy")) {
            SEARCH_QUERIES.add(query);
            SEARCH_HEADERS.add(exchange.getRequestHeaders().getFirst("Accept") + " "
                    + exchange.getRequestHeaders().getFirst("Accept-Language") + " "
                    + exchange.getRequestHeaders().getFirst("User-Agent"));
            if (query.contains("text=upstream")) {
                exchange.getResponseHeaders().set("Retry-After", "30");
                send(exchange, 429, "{}");
                return;
            }
            if (query.contains("text=forbidden")) {
                send(exchange, 403, "{\"errors\":[{\"type\":\"forbidden\"}]}");
                return;
            }
            String id = query.contains("text=detail-fallback") ? "missing" : "42";
            int page = integerParameter(query, "page", 0);
            send(exchange, 200, searchResponse(id, page));
            return;
        }
        if (path.equals("/vacancy/missing")) {
            send(exchange, 404, "{}");
            return;
        }
        send(exchange, 200, detailResponse());
    }

    private static String searchResponse(String id, int page) {
        return """
                <template id="HH-Lux-InitialState">{"vacancySearchResult":{
                  "totalResults":60,"paging":{"lastPage":{"page":2},"next":{"page":%d,"disabled":false}},
                  "vacancies":[{"vacancyId":"%s","name":"Java Developer",
                  "company":{"id":"77","visibleName":"Acme"},"links":{"desktop":"https://hh.ru/vacancy/%s"},
                  "area":{"name":"Москва"},
                  "compensation":{"from":180000,"to":230000,"currencyCode":"RUR","gross":true},
                  "snippet":{"req":"Java и Spring Boot","resp":"Backend development","skill":"Java, Spring Boot"},
                  "workExperience":"between1And3","employmentForm":"FULL","@workSchedule":"fullDay",
                  "workFormats":[{"workFormatsElement":["REMOTE"]}],
                  "publicationTime":{"$":"2026-08-20T10:00:00+0300"}}]}}
                </template>
                """.formatted(page + 1, id, id);
    }

    private static String detailResponse() {
        return """
                <template id="HH-Lux-InitialState">{"vacancyView":{
                  "vacancyId":"42","name":"Java Developer","company":{"id":"77","visibleName":"Acme"},
                  "area":{"name":"Москва"},"description":"Разрабатывать backend-сервисы",
                  "keySkills":{"keySkill":["Java","Spring Boot"]},"workExperience":"between1And3",
                  "employmentForm":"FULL","workFormats":["REMOTE"],
                  "publicationDate":"2026-08-20T10:00:00+0300"}}
                </template>
                """;
    }

    private static int integerParameter(String query, String name, int fallback) {
        for (String part : query.split("&")) {
            if (part.startsWith(name + "=")) return Integer.parseInt(part.substring(name.length() + 1));
        }
        return fallback;
    }

    private static String decode(String query) {
        return URLDecoder.decode(query == null ? "" : query, StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
