package com.ororura.analyzer.ats.domain;

import com.ororura.analyzer.ats.market.MarketSummary;

public record ResumeAtsAnalysis(
        CandidateAssessment candidate,
        MatchingAssessment matching,
        AtsScores scores,
        AnalysisInsights insights,
        MarketSummary marketData) {
}
