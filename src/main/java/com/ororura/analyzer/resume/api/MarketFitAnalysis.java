package com.ororura.analyzer.resume.api;

public record MarketFitAnalysis(Integer score, Integer mustHaveCoverage, Integer niceToHaveCoverage,
        Integer matchedVacancyPercentage, ResumeAnalysisResult.CandidateLevel targetLevel,
        ScoreBreakdown breakdown) {
}
