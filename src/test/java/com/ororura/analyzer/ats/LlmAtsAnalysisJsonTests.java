package com.ororura.analyzer.ats;

import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmAtsAnalysisJsonTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void preservesFlatLlmJsonContractAndLowercaseStatusValues() throws Exception {
        String json = objectMapper.writeValueAsString(AtsFixture.llm());

        LlmAtsAnalysis parsed = objectMapper.readValue(json, LlmAtsAnalysis.class);

        assertThat(parsed).isEqualTo(AtsFixture.llm());
        assertThat(json).contains(
                "\"detectedLevel\":\"junior_plus\"",
                "\"status\":\"confirmed_experience\"",
                "\"scoreEvidence\"",
                "\"structuredFilters\"");
    }

    @Test
    void acceptsNullableEvidenceForUnknownAndMissingValues() throws Exception {
        LlmAtsAnalysis parsed = objectMapper.readValue(
                objectMapper.writeValueAsString(AtsFixture.llm()), LlmAtsAnalysis.class);

        assertThat(parsed.structuredFilters().salary().evidence()).isNull();
        assertThat(parsed.technologies().getLast().evidence()).isNull();
    }

    @Test
    void rejectsUnknownStatusesAndDuplicateTechnologies() throws Exception {
        String json = objectMapper.writeValueAsString(AtsFixture.llm());
        String unknownStatus = json.replaceFirst("confirmed_experience", "unsupported");
        String duplicateTechnology = json.replace(
                "\"technology\":\"Spring Boot\"", "\"technology\":\"java\"");

        assertThatThrownBy(() -> objectMapper.readValue(unknownStatus, LlmAtsAnalysis.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> objectMapper.readValue(duplicateTechnology, LlmAtsAnalysis.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }
}
