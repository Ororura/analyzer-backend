package com.ororura.analyzer.resume.api;

import java.util.List;

public record VacancyFitAnalysis(Integer score, int mustHaveCoverage, int niceToHaveCoverage,
        Integer experienceFit, int gradeFit, int technicalFit, ApplyRecommendation applyRecommendation,
        List<Blocker> blockers, ScoreBreakdown breakdown) {
    public VacancyFitAnalysis { blockers = List.copyOf(blockers); }
    public record Blocker(BlockerType type, String required, String actual, BlockerSeverity severity) {}
    public enum BlockerType { EXPERIENCE, SKILL, GRADE, TECHNOLOGY }
    public enum BlockerSeverity { HIGH, MEDIUM, LOW }
    public enum ApplyRecommendation { STRONG_APPLY, APPLY, APPLY_WITH_RISK, LOW_PRIORITY, SKIP }
}
