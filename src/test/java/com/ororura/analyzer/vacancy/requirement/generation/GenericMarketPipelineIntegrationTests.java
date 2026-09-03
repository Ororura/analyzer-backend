package com.ororura.analyzer.market.profile;

import java.util.List;
import java.util.Map;

import com.ororura.analyzer.integration.polza.PolzaClient;
import com.ororura.analyzer.integration.polza.PolzaDtos.CompletionRequest;
import com.ororura.analyzer.analysis.semantic.CriterionAssessment;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.infrastructure.ai.ResumeAnalysisSchemaFactory;
import com.ororura.analyzer.resume.domain.OverallScoreCalculator;
import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.market.requirement.VacancyRequirementPipeline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "app.resume.market.min-requirement-frequency=0",
        "app.resume.market.min-vacancy-count=1",
        "app.resume.market.minimum-sample-size=1",
        "app.resume.market.batch-size=10",
        "app.resume.market.criteria.minimum-count=3",
        "app.resume.market.criteria.maximum-count=10"
})
class GenericMarketPipelineIntegrationTests {
    @Autowired VacancyRequirementPipeline requirements;
    @Autowired MarketCriterionGenerator criteria;
    @Autowired ResumeAnalysisSchemaFactory schemas;
    @Autowired OverallScoreCalculator scores;
    @MockitoBean PolzaClient polza;

    @Test
    void runsSamePipelineForJavaAndReactFixtures() {
        when(polza.requestCompletion(any())).thenAnswer(invocation -> response(invocation.getArgument(0)));

        prove(ResumeAnalysisProfile.JAVA_BACKEND, fixtures("Java", "Spring Boot", "Docker"),
                "technology:java", "technology:spring-boot", "technology:docker");
        prove(ResumeAnalysisProfile.REACT_FRONTEND, fixtures("React", "TypeScript", "Redux"),
                "technology:react", "technology:typescript", "technology:state-management");
    }

    private void prove(ResumeAnalysisProfile profile, List<Vacancy> vacancies, String first, String second, String third) {
        MarketAnalysisProfileResult generated = new MarketAnalysisProfileResult(
                criteria.generate(requirements.analyze(profile, vacancies)));
        var schemaIds = generated.profile().criteria().stream().map(value -> value.id()).toList();
        assertThat(schemas.create(generated.profile()).path("properties").path("assessments")
                .path("items").path("properties").path("criterionId").path("enum"))
                .hasSize(3).allSatisfy(value -> assertThat(schemaIds).contains(value.asString()));

        Map<String, Integer> scoreByLabel = Map.of("Primary", 8, "Secondary", 6, "Tertiary", 9);
        List<CriterionAssessment> assessments = generated.profile().criteria().stream()
                .map(value -> new CriterionAssessment(value.id(), scoreByLabel.get(value.label()), List.of("fixture")))
                .toList();
        assertThat(generated.profile().criteria().stream().collect(java.util.stream.Collectors.toMap(
                value -> value.label(), value -> value.weight())))
                .containsEntry("Primary", .5).containsEntry("Secondary", .3).containsEntry("Tertiary", .2);
        double expected = 8 * .5 + 6 * .3 + 9 * .2;
        assertThat(expected).isCloseTo(7.6, org.assertj.core.data.Offset.offset(1e-12));
        assertThat(scores.technicalScore(assessments, generated.profile().criteria())).isEqualTo(76);
    }

    private String response(CompletionRequest request) {
        String schema = request.responseFormat().jsonSchema().name();
        if (schema.equals("vacancy_requirement_extraction")) {
            var vacancies = request.messages().get(1).content().path("vacancies");
            String entries = java.util.stream.StreamSupport.stream(vacancies.spliterator(), false)
                    .map(value -> "{\"vacancyId\":\"" + value.path("vacancyId").asString()
                            + "\",\"requirements\":[]}")
                    .collect(java.util.stream.Collectors.joining(","));
            return "{\"vacancies\":[" + entries + "]}";
        }
        var allowed = request.responseFormat().jsonSchema().schema().path("properties").path("criteria")
                .path("items").path("properties").path("requirementIds").path("items").path("enum");
        List<String> ids = java.util.stream.StreamSupport.stream(allowed.spliterator(), false)
                .map(value -> value.asString()).toList();
        String primary = ids.stream().filter(value -> value.endsWith(":java") || value.endsWith(":react")).findFirst().orElseThrow();
        String secondary = ids.stream().filter(value -> value.endsWith(":spring-boot") || value.endsWith(":typescript")).findFirst().orElseThrow();
        String tertiary = ids.stream().filter(value -> value.endsWith(":docker") || value.endsWith(":state-management")).findFirst().orElseThrow();
        return grouping(primary, secondary, tertiary);
    }

    private static String grouping(String first, String second, String third) {
        return "{\"criteria\":["
                + group("Primary", first) + "," + group("Secondary", second) + "," + group("Tertiary", third) + "]}";
    }

    private static String group(String label, String id) {
        return "{\"label\":\"" + label + "\",\"description\":\"Fixture criterion " + label
                + "\",\"requirementIds\":[\"" + id + "\"]}";
    }

    private static List<Vacancy> fixtures(String primary, String secondary, String tertiary) {
        return java.util.stream.IntStream.range(0, 5).mapToObj(index -> {
            String description = primary + (index < 3 ? " " + secondary : "") + (index < 2 ? " " + tertiary : "");
            return new Vacancy("v" + index, "v" + index, primary + " Developer", "Company", null, null, null,
                    null, description, List.of(), List.of(), List.of(), null, null, null, null, "fixture", null, null);
        }).toList();
    }

    private record MarketAnalysisProfileResult(com.ororura.analyzer.market.domain.MarketAnalysisProfile profile) {}
}
