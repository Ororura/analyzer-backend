package com.ororura.analyzer.market.profile;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketCriterionGroupingValidationTests {

    private final MarketCriterionGroupingValidator validator = new MarketCriterionGroupingValidator(
            new MarketCriterionProperties(2, 10));

    @Test
    void rejectsUnknownDuplicateAndOverlappingRequirementMappings() {
        var statistics = MarketCriterionFixture.statistics();
        assertThatThrownBy(() -> validator.validate(List.of(
                MarketCriterionFixture.group("One", "Description", "unknown"),
                MarketCriterionFixture.group("Two", "Description", "technology:sql")), statistics))
                .isInstanceOf(MarketCriterionGenerationException.class).hasMessageContaining("Unknown");
        assertThatThrownBy(() -> validator.validate(List.of(
                MarketCriterionFixture.group("Same", "Description", "technology:java"),
                MarketCriterionFixture.group("same", "Description", "technology:sql")), statistics))
                .isInstanceOf(MarketCriterionGenerationException.class).hasMessageContaining("Duplicate criterion");
        assertThatThrownBy(() -> validator.validate(List.of(
                MarketCriterionFixture.group("One", "Description", "technology:java"),
                MarketCriterionFixture.group("Two", "Description", "technology:java")), statistics))
                .isInstanceOf(MarketCriterionGenerationException.class).hasMessageContaining("more than one");
    }

    @Test
    void permitsUnassignedRequirementsAndRejectsUniversalAssessments() {
        var validated = validator.validate(List.of(
                MarketCriterionFixture.group("Backend", "Description", "technology:java"),
                MarketCriterionFixture.group("Data", "Description", "technology:sql")),
                MarketCriterionFixture.statistics());
        assertThat(validated).hasSize(2);
        assertThat(validated).allSatisfy(group -> assertThat(group.requirementIds())
                .doesNotContain("tool:sonarqube"));

        assertThatThrownBy(() -> validator.validate(List.of(
                MarketCriterionFixture.group("resumeQuality", "Description", "technology:java"),
                MarketCriterionFixture.group("Data", "Description", "technology:sql")),
                MarketCriterionFixture.statistics()))
                .isInstanceOf(MarketCriterionGenerationException.class)
                .hasMessageContaining("Universal");
    }

    @Test
    void rejectsMalformedLlmResponse() {
        MarketCriterionResponseParser parser = new MarketCriterionResponseParser(new ObjectMapper());
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOf(MarketCriterionGenerationException.class);
        assertThatThrownBy(() -> parser.parse(MarketCriterionFixture.validResponse() + "{}"))
                .isInstanceOf(MarketCriterionGenerationException.class);
        assertThatThrownBy(() -> parser.parse(MarketCriterionFixture.validResponse()
                .replace("\"criteria\":[", "\"criteria\":[],\"unknown\":true,")))
                .isInstanceOf(MarketCriterionGenerationException.class);
    }
}
