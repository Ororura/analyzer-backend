package com.ororura.analyzer.vacancy.requirement.generation;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MarketCriterionPromptTests {

    @Test
    void sendsMarketStatisticsButNoResumeOrFinalWeights() {
        ObjectMapper objectMapper = new ObjectMapper();
        MarketCriterionPrompt prompt = new MarketCriterionPromptFactory(objectMapper,
                new MarketCriterionSchemaFactory(objectMapper, new MarketCriterionProperties(6, 10)))
                .create("Java Backend Developer", MarketCriterionFixture.statistics());

        assertThat(prompt.input().path("targetRole").asString()).isEqualTo("Java Backend Developer");
        assertThat(prompt.input().path("requirements")).hasSize(7);
        assertThat(prompt.input().toString()).contains("requiredFrequency", "processedCount")
                .doesNotContain("weight", "resumeText");
        assertThat(prompt.schema().toString()).doesNotContain("weight", "id\"");
        assertThat(prompt.systemInstruction()).contains("Не генерируй criterion ids, weights");
    }
}
