package com.ororura.analyzer.ats;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static com.ororura.analyzer.ats.AtsModels.*;
import static org.assertj.core.api.Assertions.assertThat;

class AtsScorerTests {

    private final AtsScorer scorer = new AtsScorer();

    @ParameterizedTest
    @CsvSource({
            "80,70,60,90,100,78",
            "0,0,0,0,0,0",
            "100,100,100,100,100,100"
    })
    void calculatesWeightedAtsScore(
            int search,
            int filters,
            int vacancy,
            int keywords,
            int readability,
            int expected) {
        assertThat(scorer.calculateAtsScore(search, filters, vacancy, keywords, readability)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"-50,0", "0,0", "49.5,50", "100,100", "149,100"})
    void clampsEveryScoreToZeroThroughOneHundred(double input, int expected) {
        assertThat(scorer.clampScore(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("structuredFilterCases")
    void preservesUnknownFilterSemantics(StructuredFilters filters, int expected) {
        assertThat(scorer.calculateStructuredFiltersScore(filters)).isEqualTo(expected);
    }

    static Stream<Arguments> structuredFilterCases() {
        FilterAssessment unknown = filter(FilterStatus.UNKNOWN);
        FilterAssessment match = filter(FilterStatus.MATCH);
        FilterAssessment partial = filter(FilterStatus.PARTIAL);
        FilterAssessment mismatch = filter(FilterStatus.MISMATCH);
        return Stream.of(
                Arguments.of(new StructuredFilters(unknown, unknown, unknown, unknown, unknown, unknown, unknown, unknown), 50),
                Arguments.of(new StructuredFilters(match, unknown, unknown, unknown, unknown, unknown, unknown, unknown), 100),
                Arguments.of(new StructuredFilters(match, partial, mismatch, unknown, unknown, unknown, unknown, unknown), 53));
    }

    @Test
    void skillsOnlyScoresBelowConfirmedExperience() {
        RawAtsAnalysis confirmed = AtsFixture.raw();
        List<RawTechnologyAssessment> skillsOnly = confirmed.technologies().stream()
                .map(item -> item.technology().equals("Java")
                        ? AtsFixture.technology("Java", TechnologyStatus.SKILLS_ONLY, item.evidence())
                        : item)
                .toList();

        assertThat(scorer.calculateKeywordCoverage(confirmed.technologies(), confirmed.detectedLevel()))
                .isGreaterThan(scorer.calculateKeywordCoverage(skillsOnly, confirmed.detectedLevel()));
    }

    @ParameterizedTest
    @CsvSource({"k8s,Kubernetes", "rabbit mq,RabbitMQ", "java 21,Java", "REST endpoints,REST API"})
    void preservesTypeScriptTechnologyAliases(String input, String expected) {
        assertThat(AtsScoringProperties.canonicalTechnology(input)).isEqualTo(expected);
    }

    @Test
    void identicalFactsProduceIdenticalFinalResult() {
        AtsAnalysisResult first = scorer.finalizeAtsAnalysis(AtsFixture.raw(), AtsFixture.baselineMarket());
        AtsAnalysisResult second = scorer.finalizeAtsAnalysis(AtsFixture.raw(), AtsFixture.baselineMarket());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void matchesTypeScriptFixtureResults() {
        AtsAnalysisResult result = scorer.finalizeAtsAnalysis(AtsFixture.raw(), AtsFixture.baselineMarket());

        assertThat(result.atsScore()).isEqualTo(66);
        assertThat(result.hhStructuredFilters()).isEqualTo(100);
        assertThat(result.vacancyMatch()).isEqualTo(46);
        assertThat(result.keywordCoverage()).isEqualTo(31);
        assertThat(result.screeningChance()).isEqualTo(ScreeningChance.MEDIUM);
        assertThat(result.keywords().explicitlyPresent()).containsExactly("Java", "PostgreSQL");
        assertThat(result.keywords().semanticallyPresent()).containsExactly("Spring Boot", "REST API");
        assertThat(result.keywords().confirmedByExperience()).containsExactly("Java", "Spring Boot", "REST API");
        assertThat(result.keywords().skillsOnly()).containsExactly("PostgreSQL");
        assertThat(result.keywords().optional()).containsExactly("Kafka", "Kubernetes");
        assertThat(result.experience().backendExperience().value()).isEqualTo("1 год 6 месяцев");
        assertThat(result.experience().relevantJavaExperience().value()).isEqualTo("6 месяцев");
    }

    private static FilterAssessment filter(FilterStatus status) {
        return new FilterAssessment(status, status == FilterStatus.UNKNOWN ? null : "evidence");
    }
}
