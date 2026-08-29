package com.ororura.analyzer.ats.domain;

import java.util.List;

public record AnalysisInsights(
        List<String> strengths,
        List<String> weaknesses,
        List<String> recruiterRisks,
        List<Recommendation> recommendations,
        String summary) {

    public AnalysisInsights {
        strengths = List.copyOf(strengths);
        weaknesses = List.copyOf(weaknesses);
        recruiterRisks = List.copyOf(recruiterRisks);
        recommendations = List.copyOf(recommendations);
    }

    public record Recommendation(
            String priority,
            String section,
            String problem,
            String recommendation,
            String example,
            String evidence) {
        public Recommendation {
            evidence = evidence == null || evidence.trim().isEmpty() ? null : evidence.trim();
        }
    }
}
