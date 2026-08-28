package com.ororura.analyzer.polza;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

import static com.ororura.analyzer.polza.PolzaDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolzaCompletionServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<String> completionContents = new ArrayDeque<>();
    private final List<JsonNode> sentRequests = new ArrayList<>();
    private HttpServer server;
    private PolzaCompletionService service;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/chat/completions", this::respond);
        server.start();
        PolzaProperties properties = new PolzaProperties(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/api/v1"),
                "google/gemini-test", "server-secret", Duration.ofSeconds(1));
        PolzaClient client = new PolzaClient(properties, objectMapper);
        service = new PolzaCompletionService(client, new AiResponseParser(objectMapper));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void parsesRealSuccessfulGeminiFixtureWithoutRepair() throws Exception {
        completionContents.add(realGeminiFixture());

        AiResumeAnalysisResponse result = service.requestAiResult(request());

        assertThat(result.atsAnalysis().hhSearchMatch()).isEqualTo(95);
        assertThat(result.atsAnalysis().technologies()).hasSize(5);
        assertThat(result.atsAnalysis().technologies().getLast().evidence()).isNull();
        assertThat(sentRequests).hasSize(1);
    }

    @Test
    void performsAtMostOneJsonRepairAttempt() throws Exception {
        completionContents.add("not-json");
        completionContents.add(realGeminiFixture());

        AiResumeAnalysisResponse result = service.requestAiResult(request());

        assertThat(result.atsAnalysis().detectedLevel().value()).isEqualTo("junior_plus");
        assertThat(sentRequests).hasSize(2);
        JsonNode repair = sentRequests.get(1);
        assertThat(repair.path("temperature").asDouble()).isZero();
        assertThat(repair.path("messages").get(0).path("content").asString())
                .contains("Исправь только JSON-структуру");
        assertThat(repair.path("messages").get(1).path("content").asString())
                .contains("INVALID_RESPONSE:\nnot-json")
                .contains("В ответе не найден JSON-объект");
    }

    @Test
    void failsAfterExactlyOneUnsuccessfulRepair() {
        completionContents.add("invalid-initial");
        completionContents.add("invalid-repair");

        assertThatThrownBy(() -> service.requestAiResult(request()))
                .isInstanceOf(AiResponseValidationException.class)
                .hasMessage("Не удалось обработать результат анализа");
        assertThat(sentRequests).hasSize(2);
    }

    private CompletionRequest request() {
        return new CompletionRequest(
                List.of(new CompletionMessage("system", StringNode.valueOf("analyze"))),
                new ResponseFormat("json_schema", new JsonSchema("resume_analysis", true, objectMapper.createObjectNode())),
                0.1,
                List.of());
    }

    private String realGeminiFixture() throws IOException {
        return Files.readString(Path.of("..", "tests", "fixtures", "gemini-ats-response.json"));
    }

    private void respond(HttpExchange exchange) throws IOException {
        sentRequests.add(objectMapper.readTree(exchange.getRequestBody()));
        String content = completionContents.removeFirst();
        String body = completionEnvelope(content);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String completionEnvelope(String content) {
        try {
            return "{\"choices\":[{\"message\":{\"content\":" + objectMapper.writeValueAsString(content) + "}}]}";
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
