package com.ororura.analyzer.resume.domain.analysis;

public record GradeFitAnalysis(CandidateLevel candidateLevel,
        CandidateLevel targetLevel, GradeFit fit, Severity severity, int score,
        ScoreBreakdown breakdown) {
    public enum GradeFit { STRONG_MATCH, MATCH, SLIGHTLY_OVERQUALIFIED, SLIGHTLY_UNDERQUALIFIED,
        OVERQUALIFIED, UNDERQUALIFIED, UNKNOWN }
    public enum Severity { NONE, LOW, MODERATE, HIGH, UNKNOWN }
}
