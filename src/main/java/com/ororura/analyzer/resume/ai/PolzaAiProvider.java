package com.ororura.analyzer.resume.ai;

import java.util.List;
import java.util.Optional;

import com.ororura.analyzer.polza.PolzaClient;
import com.ororura.analyzer.polza.PolzaClientException;
import com.ororura.analyzer.polza.PolzaDtos.CompletionMessage;
import com.ororura.analyzer.polza.PolzaDtos.CompletionRequest;
import com.ororura.analyzer.polza.PolzaDtos.JsonSchema;
import com.ororura.analyzer.polza.PolzaDtos.ResponseFormat;
import com.ororura.analyzer.polza.PolzaProperties;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;
import tools.jackson.databind.node.StringNode;

@Component
public class PolzaAiProvider implements AiProvider {

    private final PolzaClient client;
    private final ResumeAnalysisPromptFactory promptFactory;
    private final LlmResponseParser parser;
    private final PolzaProperties properties;

    public PolzaAiProvider(PolzaClient client, ResumeAnalysisPromptFactory promptFactory, LlmResponseParser parser,
            PolzaProperties properties) {
        this.client = client;
        this.promptFactory = promptFactory;
        this.parser = parser;
        this.properties = properties;
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.POLZA;
    }

    @Override
    public boolean isAvailable() {
        return properties.enabled() && properties.apiKey() != null && !properties.apiKey().isBlank();
    }

    @Override
    public Optional<String> model() {
        return Optional.ofNullable(properties.model()).filter(value -> !value.isBlank());
    }

    @Override
    public LlmResumeAnalysisResponse analyze(String resumeText, VacancyMarketData market) {
        try {
            var prompt = promptFactory.create(resumeText, market);
            CompletionRequest request = new CompletionRequest(
                    List.of(new CompletionMessage("system", StringNode.valueOf(prompt.systemInstruction())),
                            new CompletionMessage("user", prompt.input())),
                    new ResponseFormat("json_schema",
                            new JsonSchema("resume_semantic_analysis", true, prompt.schema())),
                    0,
                    List.of());
            return parser.parse(client.requestCompletion(request));
        } catch (ResumeAnalysisException exception) {
            throw exception;
        } catch (PolzaClientException exception) {
            throw map(exception);
        }
    }

    private static ResumeAnalysisException map(PolzaClientException exception) {
        if (Integer.valueOf(429).equals(exception.getStatus())) {
            return new ResumeAnalysisException(ResumeErrorCode.AI_RATE_LIMITED,
                    "AI provider rate limit exceeded", exception);
        }
        if (exception.getStatus() == null && exception.getMessage().toLowerCase(java.util.Locale.ROOT).contains("timeout")) {
            return new ResumeAnalysisException(ResumeErrorCode.AI_TIMEOUT, "AI provider timed out", exception);
        }
        return new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_UNAVAILABLE,
                "AI provider is unavailable", exception);
    }
}
