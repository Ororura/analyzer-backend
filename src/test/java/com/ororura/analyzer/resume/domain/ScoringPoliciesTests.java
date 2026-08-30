package com.ororura.analyzer.resume.domain;

import org.junit.jupiter.api.Test;

import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.InterviewChance;
import com.ororura.analyzer.resume.ai.SemanticAssessment;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringPoliciesTests {

    private final OverallScoreCalculator overall = new OverallScoreCalculator();
    private final CandidateStrengthCalculator strength = new CandidateStrengthCalculator();
    private final CandidateLevelPolicy levels = new CandidateLevelPolicy();
    private final InterviewChancePolicy chances = new InterviewChancePolicy();

    @Test
    void usesDeclaredTechnicalAndOverallWeights() {
        var criteria = new JavaBackendAnalysisProfile().criteria();
        assertThat(overall.technicalScore(scores(criteria, 10), criteria)).isEqualTo(100);
        assertThat(overall.technicalScore(scores(criteria, 0), criteria)).isZero();
        assertThat(overall.overall(80, 70, 7, 60, 6)).isEqualTo(73);
        assertThat(strength.calculate(80, 7, 6)).isEqualTo(74);
    }

    private static List<SemanticAssessment> scores(
            List<com.ororura.analyzer.resume.ai.AnalysisCriterion> criteria, int score) {
        return criteria.stream().map(value -> new SemanticAssessment(value.id(), score, List.of())).toList();
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
