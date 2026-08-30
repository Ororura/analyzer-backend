package com.ororura.analyzer.vacancy.requirement.generation;

import java.util.List;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

@Component
class MarketCriterionResponseParser {

    private final ObjectMapper objectMapper;

    MarketCriterionResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<GeneratedCriterionGroup> parse(String content) {
        try {
            return objectMapper.readerFor(MarketCriterionGroupingResponse.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .<MarketCriterionGroupingResponse>readValue(content).criteria();
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new MarketCriterionGenerationException(
                    "AI response does not match market criterion grouping schema", exception);
        }
    }
}
