package com.ororura.analyzer.resume.domain.analysis;

import java.util.List;

public record InterviewRiskAnalysis(List<InterviewRisk> topics) {
    public InterviewRiskAnalysis { topics = List.copyOf(topics); }
    public record InterviewRisk(String topic, RiskLevel risk, String sourceClaim, List<String> questions) {
        public InterviewRisk { questions = List.copyOf(questions); }
    }
    public enum RiskLevel { HIGH, MEDIUM, LOW }
}
