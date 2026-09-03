package com.ororura.analyzer.integration.polza;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

public final class PolzaDtos {

    private PolzaDtos() {
    }

    public record CompletionMessage(String role, JsonNode content) {
    }

    public record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchema jsonSchema) {
    }

    public record JsonSchema(String name, boolean strict, JsonNode schema) {
    }

    public record FileParserPlugin(String id, PdfParser pdf) {
    }

    public record PdfParser(String engine) {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record CompletionRequest(
            List<CompletionMessage> messages,
            @JsonProperty("response_format") ResponseFormat responseFormat,
            double temperature,
            List<FileParserPlugin> plugins) {

        public CompletionRequest {
            messages = List.copyOf(messages);
            plugins = plugins == null ? List.of() : List.copyOf(plugins);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record WireCompletionRequest(
            String model,
            List<CompletionMessage> messages,
            @JsonProperty("response_format") ResponseFormat responseFormat,
            double temperature,
            List<FileParserPlugin> plugins) {

        static WireCompletionRequest from(CompletionRequest request, String model) {
            return new WireCompletionRequest(model, request.messages(), request.responseFormat(), request.temperature(),
                    request.plugins());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(AssistantMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AssistantMessage(String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProviderError(ErrorBody error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(String message) {
    }
}
