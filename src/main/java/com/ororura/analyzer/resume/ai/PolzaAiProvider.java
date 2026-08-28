package com.ororura.analyzer.resume.ai;

import com.ororura.analyzer.polza.PolzaClient;
import com.ororura.analyzer.polza.PolzaClientException;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class PolzaAiProvider implements AiProvider {

    private final PolzaClient client;
    private final ResumeAnalysisPromptFactory promptFactory;
    private final LlmResponseParser parser;

    public PolzaAiProvider(PolzaClient client, ResumeAnalysisPromptFactory promptFactory, LlmResponseParser parser) {
        this.client = client;
        this.promptFactory = promptFactory;
        this.parser = parser;
    }

    @Override
    public LlmResumeAnalysisResponse analyze(String resumeText, VacancyMarketData market) {
        try {
            return parser.parse(client.requestCompletion(promptFactory.create(resumeText, market)));
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
