package com.ororura.analyzer.vacancy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VacancyMarketEndpointIntegrationTests {

    private static final HttpServer HH_SERVER = startHhServer();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void hhApiProperties(DynamicPropertyRegistry registry) {
        registry.add("app.hh.base-url", () -> "http://localhost:" + HH_SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stopHhServer() {
        HH_SERVER.stop(0);
    }

    @Test
    void returnsVacancyMarketCompatibleJson() throws Exception {
        mockMvc.perform(get("/api/vacancy-market").param("text", "Java Backend Developer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("live"))
                .andExpect(jsonPath("$.sampleSize").value(1))
                .andExpect(jsonPath("$.skillFrequencies.Java").value(1.0))
                .andExpect(jsonPath("$.skillFrequencies['Spring Boot']").value(1.0))
                .andExpect(jsonPath("$.experienceRequirements['1–3 года']").value(1))
                .andExpect(jsonPath("$.employmentTypes['Полная занятость']").value(1))
                .andExpect(jsonPath("$.workFormats['Удалённо']").value(1))
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    private static HttpServer startHhServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", VacancyMarketEndpointIntegrationTests::respond);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        String html = exchange.getRequestURI().getPath().equals("/vacancy/1")
                ? """
                  <template id="HH-Lux-InitialState">{"vacancyView":{"vacancyId":"1",
                    "keySkills":{"keySkill":["Java","Spring Boot"]},"workExperience":"between1And3",
                    "employmentForm":"FULL","workFormats":["REMOTE"]}}
                  </template>
                  """
                : """
                  <template id="HH-Lux-InitialState">{"vacancySearchResult":{"totalResults":1,
                    "paging":{"lastPage":{"page":0},"next":{"disabled":true}},
                    "vacancies":[{"vacancyId":"1","name":"Java Developer",
                    "links":{"desktop":"https://hh.ru/vacancy/1"},"area":{"name":"Москва"},
                    "snippet":{"req":"Java","skill":"Java, Spring Boot"},
                    "workExperience":"between1And3","employmentForm":"FULL","@workSchedule":"fullDay",
                    "workFormats":[{"workFormatsElement":["REMOTE"]}]}]}}
                  </template>
                  """;
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
