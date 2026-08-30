package com.ororura.analyzer.resume.ai;

import java.util.List;

public record LlmResumeAnalysisResponse(
        List<SemanticAssessment> semanticScores,
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
        semanticScores = copy(semanticScores, "semanticScores");
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

    public record ExperienceAssessment(
            SemanticScore commercialRelevance,
            SemanticScore experienceDescriptionQuality,
            SemanticScore responsibilityLevel) {
        public ExperienceAssessment {
            requireScores(commercialRelevance, experienceDescriptionQuality, responsibilityLevel);
        }
    }

    public record ResumeAssessment(SemanticScore resumeQuality, SemanticScore atsReadability) {
        public ResumeAssessment {
            requireScores(resumeQuality, atsReadability);
        }
    }

    public record SemanticScore(int score, List<String> evidence) {
        public SemanticScore {
            if (score < 0 || score > 10) {
                throw new IllegalArgumentException("Semantic score must be in range 0..10");
            }
            evidence = copy(evidence, "semanticScore.evidence");
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
            Integer startYear,
            Integer startMonth,
            Integer endYear,
            Integer endMonth,
            boolean current) {
    }

    private static void requireScores(SemanticScore... scores) {
        for (SemanticScore score : scores) {
            require(score, "semanticScore");
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
