package com.ororura.analyzer.integration.polza;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static com.ororura.analyzer.integration.polza.PolzaDtos.*;

@Component
public class PolzaClient {

    private final PolzaProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public PolzaClient(PolzaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String requestCompletion(CompletionRequest request) {
        String apiKey = properties.apiKey() == null ? "" : properties.apiKey().trim();
        if (apiKey.isEmpty()) {
            throw new PolzaClientException("POLZA_API_KEY is not configured", null);
        }

        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(WireCompletionRequest.from(request, properties.model()))
                    .exchange((sentRequest, response) -> {
                        int status = response.getStatusCode().value();
                        String body = readBody(response.getBody());
                        if (response.getStatusCode().isError()) {
                            throw providerError(body, status);
                        }
                        return completionContent(body, status);
                    });
        } catch (PolzaClientException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new PolzaClientException("Polza AI недоступен или превысил timeout", null, exception);
        } catch (RuntimeException exception) {
            throw new PolzaClientException("Не удалось выполнить запрос к Polza AI", null, exception);
        }
    }

    private String completionContent(String body, int status) {
        CompletionResponse payload;
        try {
            payload = objectMapper.readValue(body, CompletionResponse.class);
        } catch (JacksonException exception) {
            throw new PolzaClientException("Polza AI вернул некорректный ответ", status, exception);
        }
        String content = payload.choices() == null || payload.choices().isEmpty()
                || payload.choices().getFirst() == null || payload.choices().getFirst().message() == null
                ? null
                : payload.choices().getFirst().message().content();
        if (content == null || content.trim().isEmpty()) {
            throw new PolzaClientException("AI response does not contain message.content", status);
        }
        return content;
    }

    private PolzaClientException providerError(String body, int status) {
        String message = "Polza AI вернул ошибку";
        try {
            ProviderError payload = objectMapper.readValue(body, ProviderError.class);
            String providerMessage = payload.error() == null || payload.error().message() == null
                    ? ""
                    : payload.error().message().trim();
            if (!providerMessage.isEmpty()) {
                message = providerMessage;
            }
        } catch (JacksonException ignored) {
            // The generic message deliberately avoids exposing an untrusted provider body.
        }
        return new PolzaClientException(message, status);
    }

    private static String readBody(java.io.InputStream input) {
        try {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new PolzaClientException("Polza AI вернул некорректный ответ", null, exception);
        }
    }
}
