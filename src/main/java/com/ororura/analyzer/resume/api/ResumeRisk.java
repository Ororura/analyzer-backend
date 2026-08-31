package com.ororura.analyzer.resume.api;

public record ResumeRisk(RiskType type, RiskSeverity severity, String subjectKey, String title,
        String evidence, String impact, String action) {
    public enum RiskType { SKILL_GAP, ATS, EXPERIENCE, GRADE, CLAIM, VACANCY_BLOCKER, DATA_QUALITY }
    public enum RiskSeverity { CRITICAL, HIGH, MEDIUM, LOW }
}
