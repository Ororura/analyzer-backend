package com.ororura.analyzer.vacancy.requirement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

@Component
class VacancyRequirementResponseParser {

    private final ObjectMapper objectMapper;

    VacancyRequirementResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<ExtractedVacancyRequirements> parse(String content, List<String> expectedVacancyIds) {
        try {
            VacancyRequirementExtractionResponse response = objectMapper.readerFor(
                            VacancyRequirementExtractionResponse.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(content);
            validateMapping(response.vacancies(), expectedVacancyIds);
            return response.vacancies();
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new VacancyRequirementExtractionException("AI response does not match vacancy extraction schema",
                    exception);
        }
    }

    private static void validateMapping(List<ExtractedVacancyRequirements> extracted, List<String> expected) {
        Set<String> expectedIds = Set.copyOf(expected);
        Set<String> actualIds = new HashSet<>();
        for (ExtractedVacancyRequirements vacancy : extracted) {
            if (!actualIds.add(vacancy.vacancyId())) {
                throw new IllegalArgumentException("Duplicate vacancyId in AI response");
            }
        }
        if (actualIds.size() != extracted.size() || !actualIds.equals(expectedIds)) {
            throw new IllegalArgumentException("AI response vacancyIds do not match input");
        }
    }
}
