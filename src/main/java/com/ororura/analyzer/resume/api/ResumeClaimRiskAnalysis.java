package com.ororura.analyzer.resume.api;

import java.util.List;

public record ResumeClaimRiskAnalysis(List<ClaimRisk> claims) {
    public ResumeClaimRiskAnalysis { claims = List.copyOf(claims); }
    public record ClaimRisk(String claim, int valueScore, int credibilityScore, int interviewRisk,
            int evidenceQuality, ClaimRecommendation recommendation, String explanation) {
    }
    public enum ClaimRecommendation { KEEP, KEEP_AND_PREPARE, CLARIFY, SOFTEN, REMOVE }
}
