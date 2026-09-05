package com.ororura.analyzer.resume.api;

public record GradeFitAnalysis(ResumeAnalysisResult.CandidateLevel candidateLevel,
        ResumeAnalysisResult.CandidateLevel targetLevel, GradeFit fit, Severity severity, int score,
        ScoreBreakdown breakdown) {
    @com.fasterxml.jackson.annotation.JsonProperty(value="targetGrade", access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public com.ororura.analyzer.analysis.domain.CandidateGrade targetGrade() {
        return com.ororura.analyzer.analysis.domain.CandidateGrade.fromLegacy(targetLevel);
    }
    @com.fasterxml.jackson.annotation.JsonProperty(value="detectedGrade", access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public com.ororura.analyzer.analysis.domain.CandidateGrade detectedGrade() {
        return com.ororura.analyzer.analysis.domain.CandidateGrade.fromLegacy(candidateLevel);
    }
    public enum GradeFit { STRONG_MATCH, MATCH, SLIGHTLY_OVERQUALIFIED, SLIGHTLY_UNDERQUALIFIED,
        OVERQUALIFIED, UNDERQUALIFIED, UNKNOWN }
    public enum Severity { NONE, LOW, MODERATE, HIGH, UNKNOWN }
}
