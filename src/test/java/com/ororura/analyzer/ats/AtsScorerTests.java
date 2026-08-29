package com.ororura.analyzer.ats;

import java.util.List;
import java.util.stream.Stream;

import com.ororura.analyzer.ats.domain.AtsScores.ScreeningChance;
import com.ororura.analyzer.ats.domain.MatchingAssessment.FilterAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.FilterStatus;
import com.ororura.analyzer.ats.domain.MatchingAssessment.ResumeFilterAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyStatus;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis.NormalizedTechnologyAssessment;
import com.ororura.analyzer.ats.domain.ResumeAtsAnalysis;
import com.ororura.analyzer.ats.scoring.AtsScorer;
import com.ororura.analyzer.ats.scoring.AtsScoringProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

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
    void preservesUnknownFilterSemantics(ResumeFilterAssessment filters, int expected) {
        assertThat(scorer.calculateStructuredFiltersScore(filters)).isEqualTo(expected);
    }

    static Stream<Arguments> structuredFilterCases() {
        FilterAssessment unknown = filter(FilterStatus.UNKNOWN);
        FilterAssessment match = filter(FilterStatus.MATCH);
        FilterAssessment partial = filter(FilterStatus.PARTIAL);
        FilterAssessment mismatch = filter(FilterStatus.MISMATCH);
        return Stream.of(
                Arguments.of(filters(unknown, unknown, unknown), 50),
                Arguments.of(filters(match, unknown, unknown), 100),
                Arguments.of(filters(match, partial, mismatch), 53));
    }

    @Test
    void skillsOnlyScoresBelowConfirmedExperience() {
        NormalizedAtsAnalysis confirmed = AtsFixture.normalized();
        List<NormalizedTechnologyAssessment> skillsOnly = confirmed.technologies().stream()
                .map(item -> item.technology().equals("Java")
                        ? AtsFixture.technology("Java", TechnologyStatus.SKILLS_ONLY, item.evidence())
                        : item)
                .toList();

        assertThat(scorer.calculateKeywordCoverage(confirmed.technologies(), confirmed.candidate().detectedLevel()))
                .isGreaterThan(scorer.calculateKeywordCoverage(
                        skillsOnly, confirmed.candidate().detectedLevel()));
    }

    @ParameterizedTest
    @CsvSource({"k8s,Kubernetes", "rabbit mq,RabbitMQ", "java 21,Java", "REST endpoints,REST API"})
    void preservesTypeScriptTechnologyAliases(String input, String expected) {
        assertThat(AtsScoringProperties.canonicalTechnology(input)).isEqualTo(expected);
    }

    @Test
    void identicalFactsProduceIdenticalFinalResult() {
        ResumeAtsAnalysis first = scorer.finalizeAtsAnalysis(AtsFixture.normalized(), AtsFixture.baselineMarket());
        ResumeAtsAnalysis second = scorer.finalizeAtsAnalysis(AtsFixture.normalized(), AtsFixture.baselineMarket());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void matchesTypeScriptFixtureResults() {
        ResumeAtsAnalysis result = scorer.finalizeAtsAnalysis(AtsFixture.normalized(), AtsFixture.baselineMarket());

        assertThat(result.scores().atsScore()).isEqualTo(66);
        assertThat(result.scores().hhStructuredFilters()).isEqualTo(100);
        assertThat(result.scores().vacancyMatch()).isEqualTo(46);
        assertThat(result.scores().keywordCoverage()).isEqualTo(31);
        assertThat(result.scores().screeningChance()).isEqualTo(ScreeningChance.MEDIUM);
        assertThat(result.matching().keywords().explicitlyPresent()).containsExactly("Java", "PostgreSQL");
        assertThat(result.matching().keywords().semanticallyPresent()).containsExactly("Spring Boot", "REST API");
        assertThat(result.matching().keywords().confirmedByExperience())
                .containsExactly("Java", "Spring Boot", "REST API");
        assertThat(result.matching().keywords().skillsOnly()).containsExactly("PostgreSQL");
        assertThat(result.matching().keywords().optional()).containsExactly("Kafka", "Kubernetes");
        assertThat(result.candidate().experience().backendExperience().value()).isEqualTo("1 год 6 месяцев");
        assertThat(result.candidate().experience().relevantJavaExperience().value()).isEqualTo("6 месяцев");
    }

    private static ResumeFilterAssessment filters(
            FilterAssessment experience,
            FilterAssessment education,
            FilterAssessment location) {
        FilterAssessment unknown = filter(FilterStatus.UNKNOWN);
        return new ResumeFilterAssessment(
                experience, education, location, unknown, unknown, unknown, unknown, unknown);
    }

    private static FilterAssessment filter(FilterStatus status) {
        return new FilterAssessment(status, status == FilterStatus.UNKNOWN ? null : "evidence");
    }
}
