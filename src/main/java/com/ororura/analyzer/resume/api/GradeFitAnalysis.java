package com.ororura.analyzer.resume.api;

public record GradeFitAnalysis(ResumeAnalysisResult.CandidateLevel candidateLevel,
        ResumeAnalysisResult.CandidateLevel targetLevel, GradeFit fit, Severity severity, int score,
        ScoreBreakdown breakdown) {
    public enum GradeFit { STRONG_MATCH, MATCH, SLIGHTLY_OVERQUALIFIED, SLIGHTLY_UNDERQUALIFIED,
        OVERQUALIFIED, UNDERQUALIFIED, UNKNOWN }
    public enum Severity { NONE, LOW, MODERATE, HIGH, UNKNOWN }
}
