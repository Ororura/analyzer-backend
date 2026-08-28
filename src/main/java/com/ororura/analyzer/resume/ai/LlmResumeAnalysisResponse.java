package com.ororura.analyzer.resume.ai;

import java.util.List;

public record LlmResumeAnalysisResponse(
        TechnicalAssessment technicalAssessment,
        ExperienceAssessment experienceAssessment,
        ResumeAssessment resumeAssessment,
        Skills skills,
        List<EmploymentPeriod> employmentPeriods,
        List<String> strengths,
        List<String> weaknesses,
        List<String> atsIssues,
        List<String> recommendations,
        List<String> warnings) {

    public LlmResumeAnalysisResponse {
        require(technicalAssessment, "technicalAssessment");
        require(experienceAssessment, "experienceAssessment");
        require(resumeAssessment, "resumeAssessment");
        require(skills, "skills");
        employmentPeriods = copy(employmentPeriods, "employmentPeriods");
        strengths = copy(strengths, "strengths");
        weaknesses = copy(weaknesses, "weaknesses");
        atsIssues = copy(atsIssues, "atsIssues");
        recommendations = copy(recommendations, "recommendations");
        warnings = copy(warnings, "warnings");
    }

    public record TechnicalAssessment(
            int javaDepth,
            int springDepth,
            int backendDepth,
            int sqlPostgresqlDepth,
            int hibernateJpaDepth,
            int infrastructureDepth,
            int messagingCacheDepth,
            int testingDepth) {
        public TechnicalAssessment {
            scores(javaDepth, springDepth, backendDepth, sqlPostgresqlDepth, hibernateJpaDepth,
                    infrastructureDepth, messagingCacheDepth, testingDepth);
        }
    }

    public record ExperienceAssessment(
            int commercialRelevance,
            int experienceDescriptionQuality,
            int responsibilityLevel) {
        public ExperienceAssessment {
            scores(commercialRelevance, experienceDescriptionQuality, responsibilityLevel);
        }
    }

    public record ResumeAssessment(int resumeQuality, int atsReadability) {
        public ResumeAssessment {
            scores(resumeQuality, atsReadability);
        }
    }

    public record Skills(List<String> confirmed, List<String> weakEvidence, List<String> missing) {
        public Skills {
            confirmed = copy(confirmed, "skills.confirmed");
            weakEvidence = copy(weakEvidence, "skills.weakEvidence");
            missing = copy(missing, "skills.missing");
        }
    }

    public record EmploymentPeriod(
            String company,
            String position,
            int startYear,
            int startMonth,
            Integer endYear,
            Integer endMonth,
            boolean current) {
    }

    private static void scores(int... scores) {
        for (int score : scores) {
            if (score < 0 || score > 10) throw new IllegalArgumentException("Semantic score must be in range 0..10");
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static <T> List<T> copy(List<T> values, String name) {
        require(values, name);
        if (values.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException(name + " must not contain null");
        }
        return List.copyOf(values);
    }
}
