package com.ororura.analyzer.ats.domain;

import java.util.List;

public record AtsScores(
        ScoreAssessment hhSearchMatch,
        ScoreAssessment recruiterReadability,
        ScoreAssessment targetLevelFit,
        int atsScore,
        int hhStructuredFilters,
        int vacancyMatch,
        int keywordCoverage,
        ScreeningChance screeningChance) {

    public record ScoreAssessment(int score, List<String> evidence) {
        public ScoreAssessment {
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("score must be in range 0..100");
            }
            evidence = List.copyOf(evidence);
        }
    }

    public enum ScreeningChance {
        LOW, BELOW_AVERAGE, MEDIUM, HIGH, VERY_HIGH
    }
}
