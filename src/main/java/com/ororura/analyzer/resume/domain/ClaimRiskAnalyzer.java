package com.ororura.analyzer.resume.domain;

import java.util.Comparator;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.api.InterviewRiskAnalysis;
import com.ororura.analyzer.resume.api.ResumeClaimRiskAnalysis;
import org.springframework.stereotype.Component;

@Component
public class ClaimRiskAnalyzer {
    public ResumeClaimRiskAnalysis claims(LlmResumeAnalysisResponse llm) {
        return new ResumeClaimRiskAnalysis(llm.claims().stream().map(value ->
                new ResumeClaimRiskAnalysis.ClaimRisk(value.claim(), value.valueScore(), value.credibilityScore(),
                        value.interviewRisk(), value.evidenceQuality(), recommendation(value), value.explanation()))
                .sorted(Comparator.comparingInt(ResumeClaimRiskAnalysis.ClaimRisk::interviewRisk).reversed())
                .toList());
    }

    public InterviewRiskAnalysis interviews(LlmResumeAnalysisResponse llm) {
        java.util.List<InterviewRiskAnalysis.InterviewRisk> topics = new java.util.ArrayList<>();
        int remaining = 15;
        for (var value : llm.interviewTopics()) {
            if (remaining == 0) break;
            if (value.topic() == null || value.topic().isBlank()) continue;
            var questions = value.questions().stream().distinct().limit(Math.min(3, remaining)).toList();
            if (questions.isEmpty()) continue;
            topics.add(new InterviewRiskAnalysis.InterviewRisk(value.topic(), risk(value.sourceClaim(), llm),
                    value.sourceClaim(), questions));
            remaining -= questions.size();
        }
        return new InterviewRiskAnalysis(topics);
    }

    private static ResumeClaimRiskAnalysis.ClaimRecommendation recommendation(
            LlmResumeAnalysisResponse.ClaimFact value) {
        if (value.valueScore() < 4 && value.credibilityScore() < 4) return ResumeClaimRiskAnalysis.ClaimRecommendation.REMOVE;
        if (value.credibilityScore() <= 3) return ResumeClaimRiskAnalysis.ClaimRecommendation.SOFTEN;
        if (value.credibilityScore() <= 6) return ResumeClaimRiskAnalysis.ClaimRecommendation.CLARIFY;
        if (value.interviewRisk() >= 5) return ResumeClaimRiskAnalysis.ClaimRecommendation.KEEP_AND_PREPARE;
        return ResumeClaimRiskAnalysis.ClaimRecommendation.KEEP;
    }

    private static InterviewRiskAnalysis.RiskLevel risk(String claim, LlmResumeAnalysisResponse llm) {
        int score = llm.claims().stream().filter(value -> value.claim().equals(claim)).findFirst()
                .map(LlmResumeAnalysisResponse.ClaimFact::interviewRisk).orElse(5);
        return score >= 7 ? InterviewRiskAnalysis.RiskLevel.HIGH
                : score >= 4 ? InterviewRiskAnalysis.RiskLevel.MEDIUM : InterviewRiskAnalysis.RiskLevel.LOW;
    }
}
