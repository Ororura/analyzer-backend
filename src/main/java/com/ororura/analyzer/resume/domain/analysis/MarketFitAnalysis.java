package com.ororura.analyzer.resume.domain.analysis;

public record MarketFitAnalysis(Integer score, Integer mustHaveCoverage, Integer niceToHaveCoverage,
        Integer matchedVacancyPercentage, CandidateLevel targetLevel,
        ScoreBreakdown breakdown) {
}
