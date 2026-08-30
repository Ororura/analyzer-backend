package com.ororura.analyzer.resume.domain;

import org.junit.jupiter.api.Test;

import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.InterviewChance;
import com.ororura.analyzer.resume.ai.CriterionAssessment;
import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoringPoliciesTests {

    private final OverallScoreCalculator overall = new OverallScoreCalculator();
    private final CandidateStrengthCalculator strength = new CandidateStrengthCalculator();
    private final CandidateLevelPolicy levels = new CandidateLevelPolicy();
    private final InterviewChancePolicy chances = new InterviewChancePolicy();

    @Test
    void usesDeclaredTechnicalAndOverallWeights() {
        var criteria = List.of(
                new AnalysisCriterion("one", "One", "First", .6, .8),
                new AnalysisCriterion("two", "Two", "Second", .4, .6));
        assertThat(overall.technicalScore(scores(criteria, 10), criteria)).isEqualTo(100);
        assertThat(overall.technicalScore(scores(criteria, 0), criteria)).isZero();
        assertThat(overall.overall(80, 70, 7, 60, 6)).isEqualTo(73);
        assertThat(strength.calculate(80, 7, 6)).isEqualTo(74);
    }

    @Test
    void calculatesCompletelyDifferentCriterionIdsWithoutProfileSpecificKnowledge() {
        List<AnalysisCriterion> criteria = List.of(
                new AnalysisCriterion("react", "React", "React delivery", .5, .8),
                new AnalysisCriterion("typescript", "TypeScript", "Typed frontend", .3, .7),
                new AnalysisCriterion("frontend-architecture", "Architecture", "Frontend architecture", .2, .4));
        List<CriterionAssessment> assessments = List.of(
                new CriterionAssessment("react", 10, List.of()),
                new CriterionAssessment("typescript", 5, List.of()),
                new CriterionAssessment("frontend-architecture", 0, List.of()));

        assertThat(overall.technicalScore(assessments, criteria)).isEqualTo(65);
    }

    @Test
    void rejectsNonNormalizedProfileWeightsInsteadOfSilentlyRenormalizing() {
        List<AnalysisCriterion> criteria = List.of(
                new AnalysisCriterion("one", "One", "First", .4, .5),
                new AnalysisCriterion("two", "Two", "Second", .4, .5));
        assertThatThrownBy(() -> overall.technicalScore(scores(criteria, 5), criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("normalized");
    }

    private static List<CriterionAssessment> scores(
            List<com.ororura.analyzer.resume.ai.AnalysisCriterion> criteria, int score) {
        return criteria.stream().map(value -> new CriterionAssessment(value.id(), score, List.of())).toList();
    }

    @Test
    void appliesLevelBoundariesFromHighestToLowest() {
        assertThat(levels.detect(36, 75, 7, 75)).isEqualTo(CandidateLevel.MIDDLE);
        assertThat(levels.detect(24, 65, 6, 65)).isEqualTo(CandidateLevel.MIDDLE_MINUS);
        assertThat(levels.detect(12, 55, 0, 55)).isEqualTo(CandidateLevel.JUNIOR_PLUS);
        assertThat(levels.detect(11, 100, 10, 100)).isEqualTo(CandidateLevel.JUNIOR);
    }

    @Test
    void appliesInterviewChanceBoundaries() {
        assertThat(chances.hr(75, 65)).isEqualTo(InterviewChance.HIGH);
        assertThat(chances.hr(50, 45)).isEqualTo(InterviewChance.MEDIUM);
        assertThat(chances.hr(49, 100)).isEqualTo(InterviewChance.LOW);
        assertThat(chances.technical(75, 70)).isEqualTo(InterviewChance.HIGH);
        assertThat(chances.technical(55, 50)).isEqualTo(InterviewChance.MEDIUM);
        assertThat(chances.technical(54, 100)).isEqualTo(InterviewChance.LOW);
    }
}
