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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

import static com.ororura.analyzer.polza.PolzaDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolzaClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<MockResponse> responses = new ArrayDeque<>();
    private final List<CapturedRequest> requests = new ArrayList<>();
    private HttpServer server;
    private PolzaClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/chat/completions", this::respond);
        server.start();
        client = createClient(Duration.ofSeconds(1));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsAuthorizationAndConfiguredModelAndReturnsContent() throws Exception {
        responses.add(json(200, completionEnvelope("completion")));

        assertThat(client.requestCompletion(request())).isEqualTo("completion");

        CapturedRequest sent = requests.getFirst();
        assertThat(sent.authorization()).isEqualTo("Bearer server-secret");
        assertThat(sent.contentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(sent.body());
        assertThat(body.path("model").asString()).isEqualTo("google/gemini-test");
        assertThat(body.path("messages").get(0).path("content").asString()).isEqualTo("test");
    }

    @ParameterizedTest
    @MethodSource("providerErrors")
    void mapsProviderErrors(int status, String body, String expectedMessage) {
        responses.add(json(status, body));

        assertThatThrownBy(() -> client.requestCompletion(request()))
                .isInstanceOf(PolzaClientException.class)
                .hasMessage(expectedMessage)
                .extracting(error -> ((PolzaClientException) error).getStatus())
                .isEqualTo(status);
    }

    static List<Arguments> providerErrors() {
        return List.of(
                Arguments.of(401, "{\"error\":{\"message\":\"Invalid API key\"}}", "Invalid API key"),
                Arguments.of(429, "{\"error\":{\"message\":\"Rate limited\"}}", "Rate limited"),
                Arguments.of(400, "{\"error\":{}}", "Polza AI вернул ошибку"),
                Arguments.of(503, "not-json", "Polza AI вернул ошибку"));
    }

    @ParameterizedTest
    @MethodSource("missingContentResponses")
    void rejectsMissingFirstChoiceContent(String responseBody) {
        responses.add(json(200, responseBody));

        assertThatThrownBy(() -> client.requestCompletion(request()))
                .isInstanceOf(PolzaClientException.class)
                .hasMessage("AI response does not contain message.content")
                .extracting(error -> ((PolzaClientException) error).getStatus())
                .isEqualTo(200);
    }

    static List<String> missingContentResponses() {
        return List.of("{}", "{\"choices\":[{\"message\":{}}]}",
                "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}");
    }

    @Test
    void rejectsMalformedProviderJson() {
        responses.add(json(200, "{invalid"));

        assertThatThrownBy(() -> client.requestCompletion(request()))
                .isInstanceOf(PolzaClientException.class)
                .hasMessage("Polza AI вернул некорректный ответ");
    }

    @Test
    void enforcesConfiguredTimeout() {
        responses.add(new MockResponse(200, completionEnvelope("late"), 200));
        PolzaClient shortTimeoutClient = createClient(Duration.ofMillis(30));

        assertThatThrownBy(() -> shortTimeoutClient.requestCompletion(request()))
                .isInstanceOf(PolzaClientException.class)
                .hasMessage("Polza AI недоступен или превысил timeout");
    }

    @Test
    void readsApiKeyOnlyFromServerConfiguration() {
        PolzaClient missingKeyClient = new PolzaClient(
                new PolzaProperties(baseUrl(), "google/gemini-test", "", Duration.ofSeconds(1)), objectMapper);

        assertThatThrownBy(() -> missingKeyClient.requestCompletion(request()))
                .isInstanceOf(PolzaClientException.class)
                .hasMessage("POLZA_API_KEY is not configured");
        assertThat(requests).isEmpty();
    }

    private PolzaClient createClient(Duration timeout) {
        return new PolzaClient(
                new PolzaProperties(baseUrl(), "google/gemini-test", "server-secret", timeout), objectMapper);
    }

    private URI baseUrl() {
        return URI.create("http://localhost:" + server.getAddress().getPort() + "/api/v1");
    }

    private CompletionRequest request() {
        return new CompletionRequest(
                List.of(new CompletionMessage("system", StringNode.valueOf("test"))),
                new ResponseFormat("json_schema", new JsonSchema("resume_analysis", true, objectMapper.createObjectNode())),
                0,
                List.of());
    }

    private void respond(HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        requests.add(new CapturedRequest(
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                new String(requestBody, StandardCharsets.UTF_8)));
        MockResponse response = responses.removeFirst();
        if (response.delayMillis() > 0) {
            try {
                Thread.sleep(response.delayMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(response.status(), body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private MockResponse json(int status, String body) {
        return new MockResponse(status, body, 0);
    }

    private String completionEnvelope(String content) {
        try {
            return "{\"choices\":[{\"message\":{\"content\":" + objectMapper.writeValueAsString(content) + "}}]}";
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record MockResponse(int status, String body, long delayMillis) {
    }

    private record CapturedRequest(String authorization, String contentType, String body) {
    }
}
