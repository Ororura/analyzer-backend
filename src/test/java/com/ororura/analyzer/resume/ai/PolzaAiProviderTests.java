package com.ororura.analyzer.resume.ai;

import java.util.List;
import java.util.Map;
import java.net.URI;
import java.time.Duration;

import com.ororura.analyzer.polza.PolzaClient;
import com.ororura.analyzer.polza.PolzaClientException;
import com.ororura.analyzer.polza.PolzaProperties;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class PolzaAiProviderTests {

    private final PolzaClient client = mock(PolzaClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PolzaAiProvider provider = new PolzaAiProvider(client,
            new ResumeAnalysisPromptFactory(objectMapper, new ResumeAnalysisSchemaFactory(objectMapper)),
            new LlmResponseParser(objectMapper),
            new PolzaProperties(true, URI.create("https://polza.ai/api/v1"), "test-model", "secret",
                    Duration.ofSeconds(1)));

    @Test
    void parsesSuccessfulResponseWithoutRepairRequest() {
        when(client.requestCompletion(any())).thenReturn(LlmBoundaryTests.validJson());
        assertThat(provider.analyze(LlmBoundaryTests.javaProfile(), "resume", market())
                .assessments().getFirst().score()).isEqualTo(8);
    }

    @Test
    void mapsRateLimitTimeoutAndOtherProviderFailures() {
        assertMapped(new PolzaClientException("limited", 429), ResumeErrorCode.AI_RATE_LIMITED);
        assertMapped(new PolzaClientException("provider timeout", null), ResumeErrorCode.AI_TIMEOUT);
        assertMapped(new PolzaClientException("unauthorized", 401), ResumeErrorCode.AI_PROVIDER_UNAVAILABLE);
        assertMapped(new PolzaClientException("down", 503), ResumeErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    private void assertMapped(PolzaClientException source, ResumeErrorCode expected) {
        doThrow(source).when(client).requestCompletion(any());
        assertThatThrownBy(() -> provider.analyze(LlmBoundaryTests.javaProfile(), "resume", market()))
                .isInstanceOf(ResumeAnalysisException.class)
                .extracting(error -> ((ResumeAnalysisException) error).getCode())
                .isEqualTo(expected);
    }

    private static VacancyMarketData market() {
        return new VacancyMarketData("test", 1, Map.of(), Map.of(), Map.of(), Map.of(), List.of());
    }
}
