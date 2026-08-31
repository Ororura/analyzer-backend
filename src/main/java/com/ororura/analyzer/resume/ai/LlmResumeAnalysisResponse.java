package com.ororura.analyzer.resume.ai;

import java.util.List;

public record LlmResumeAnalysisResponse(
        List<CriterionAssessment> assessments,
        ExperienceAssessment experienceAssessment,
        ResumeAssessment resumeAssessment,
        Skills skills,
        List<SkillEvidenceFact> skillEvidence,
        List<AtsFieldFact> atsFields,
        List<ClaimFact> claims,
        List<InterviewTopicFact> interviewTopics,
        List<EmploymentPeriod> employmentPeriods,
        List<String> strengths,
        List<String> weaknesses,
        List<String> atsIssues,
        List<String> recommendations,
        List<String> warnings) {

    public LlmResumeAnalysisResponse {
        assessments = copy(assessments, "assessments");
        require(experienceAssessment, "experienceAssessment");
        require(resumeAssessment, "resumeAssessment");
        require(skills, "skills");
        skillEvidence = copyNullable(skillEvidence);
        atsFields = copyNullable(atsFields);
        claims = copyNullable(claims);
        interviewTopics = copyNullable(interviewTopics);
        employmentPeriods = copy(employmentPeriods, "employmentPeriods");
        strengths = copy(strengths, "strengths");
        weaknesses = copy(weaknesses, "weaknesses");
        atsIssues = copy(atsIssues, "atsIssues");
        recommendations = copy(recommendations, "recommendations");
        warnings = copy(warnings, "warnings");
    }

    public LlmResumeAnalysisResponse(List<CriterionAssessment> assessments,
            ExperienceAssessment experienceAssessment, ResumeAssessment resumeAssessment, Skills skills,
            List<EmploymentPeriod> employmentPeriods, List<String> strengths, List<String> weaknesses,
            List<String> atsIssues, List<String> recommendations, List<String> warnings) {
        this(assessments, experienceAssessment, resumeAssessment, skills, List.of(), List.of(), List.of(), List.of(),
                employmentPeriods, strengths, weaknesses, atsIssues, recommendations, warnings);
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

    public record SkillEvidenceFact(String skill, List<EvidenceFact> evidence) {
        public SkillEvidenceFact { evidence = copy(evidence, "skillEvidence.evidence"); }
    }

    public record EvidenceFact(String text, EvidenceContext context, boolean concrete, boolean hasOutcome) {
    }

    public enum EvidenceContext { COMMERCIAL_TASK, PROJECT_TASK, STACK_CONTEXT, SKILLS_SECTION }

    public record AtsFieldFact(String field, AtsFieldStatus status, List<String> issues) {
        public AtsFieldFact { issues = copy(issues, "atsFields.issues"); }
    }

    public enum AtsFieldStatus { OK, PARTIAL, FAILED }

    public record ClaimFact(String claim, int valueScore, int credibilityScore, int interviewRisk,
            int evidenceQuality, String explanation) {
        public ClaimFact {
            requireRange(valueScore); requireRange(credibilityScore); requireRange(interviewRisk);
            requireRange(evidenceQuality);
        }
    }

    public record InterviewTopicFact(String topic, String sourceClaim, List<String> questions) {
        public InterviewTopicFact { questions = copy(questions, "interviewTopics.questions"); }
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

    private static <T> List<T> copyNullable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static void requireRange(int score) {
        if (score < 0 || score > 10) throw new IllegalArgumentException("Claim score must be in range 0..10");
    }
}
