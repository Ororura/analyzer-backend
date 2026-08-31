package com.ororura.analyzer.resume.api;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;
import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.ai.CriterionAssessment;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketProfileSource;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ResumeAnalysisResult")
public record ResumeAnalysisResult(
        String targetRole,
        @Schema(deprecated = true) CandidateLevel detectedLevel,
        Scores scores,
        int overallScore,
        int candidateStrength,
        InterviewChance hrScreeningChance,
        InterviewChance technicalInterviewChance,
        Experience experience,
        @Schema(deprecated = true) Skills skills,
        List<String> strengths,
        @Schema(deprecated = true) List<String> weaknesses,
        @Schema(deprecated = true) List<String> atsIssues,
        @Schema(deprecated = true) List<String> recommendations,
        VacancyFitAnalysis vacancyFit,
        Market market,
        Metadata metadata,
        MarketFitAnalysis marketFit,
        MarketPosition marketPosition,
        TechnicalProfile technicalProfile,
        SkillEvidenceAnalysis skillEvidence,
        SkillGapAnalysis skillGaps,
        SkillRoiAnalysis skillRoi,
        GradeFitAnalysis gradeFit,
        AtsAnalysis ats,
        ResumeClaimRiskAnalysis claimRisks,
        InterviewRiskAnalysis interviewRisks,
        List<ResumeRisk> risks,
        ResumeRecommendationAnalysis recommendationAnalysis,
        String markdownReport,
        List<String> warnings) {

    public ResumeAnalysisResult {
        strengths = List.copyOf(strengths);
        weaknesses = List.copyOf(weaknesses);
        atsIssues = List.copyOf(atsIssues);
        recommendations = List.copyOf(recommendations);
        risks = risks == null ? List.of() : List.copyOf(risks);
        warnings = List.copyOf(warnings);
    }

    public ResumeAnalysisResult(String targetRole, CandidateLevel detectedLevel, Scores scores, int overallScore,
            int candidateStrength, InterviewChance hrScreeningChance, InterviewChance technicalInterviewChance,
            Experience experience, Skills skills, List<String> strengths, List<String> weaknesses,
            List<String> atsIssues, List<String> recommendations, Market market, Metadata metadata,
            List<String> warnings) {
        this(targetRole, detectedLevel, scores, overallScore, candidateStrength, hrScreeningChance,
                technicalInterviewChance, experience, skills, strengths, weaknesses, atsIssues, recommendations,
                null, market, metadata, null, null, null, null, null, null, null, null, null, null,
                List.of(), null, null, warnings);
    }

    public ResumeAnalysisResult withMarkdownReport(String markdown) {
        return new ResumeAnalysisResult(targetRole, detectedLevel, scores, overallScore, candidateStrength,
                hrScreeningChance, technicalInterviewChance, experience, skills, strengths, weaknesses, atsIssues,
                recommendations, vacancyFit, market, metadata, marketFit, marketPosition, technicalProfile,
                skillEvidence, skillGaps, skillRoi, gradeFit, ats, claimRisks, interviewRisks, risks,
                recommendationAnalysis, markdown, warnings);
    }

    public record Scores(
            List<CriterionAssessment> assessments,
            int commercialExperience,
            int experienceDescription,
            int ats,
            int resumeQuality) {
        public Scores {
            assessments = List.copyOf(assessments);
        }
    }

    public record Experience(int commercialMonths, int commercialYears, int remainingMonths) {
    }

    public record Skills(List<String> confirmed, List<String> weakEvidence, List<String> missing) {
        public Skills {
            confirmed = List.copyOf(confirmed);
            weakEvidence = List.copyOf(weakEvidence);
            missing = List.copyOf(missing);
        }
    }

    public record Market(String source, int sampleSize) {
    }

    @Deprecated
    public record VacancyFit(
            List<String> requiredSkills,
            List<String> optionalSkills,
            List<String> missingSkills,
            int experienceRelevanceScore,
            String candidateLevelFit,
            List<String> risks,
            List<String> probableRejectionReasons) {
        public VacancyFit {
            requiredSkills = List.copyOf(requiredSkills);
            optionalSkills = List.copyOf(optionalSkills);
            missingSkills = List.copyOf(missingSkills);
            risks = List.copyOf(risks);
            probableRejectionReasons = List.copyOf(probableRejectionReasons);
        }
    }

    public record Metadata(int analysisSchemaVersion, ResumeAnalysisProfile analysisProfile, String analysisVersion,
            String baselineVersion, String marketProfileVersion,
            MarketProfileSource marketProfileSource, Instant generatedAt, AiProviderType provider, String model) {

        public Metadata(String analysisVersion, String baselineVersion, String marketProfileVersion,
                Instant generatedAt, AiProviderType provider, String model) {
            this(2, null, analysisVersion, baselineVersion, marketProfileVersion, null, generatedAt, provider, model);
        }

        public Metadata(String analysisVersion, String baselineVersion, Instant generatedAt,
                AiProviderType provider, String model) {
            this(2, null, analysisVersion, baselineVersion, null, null, generatedAt, provider, model);
        }

        public Metadata(ResumeAnalysisProfile analysisProfile, String analysisVersion, String baselineVersion,
                String marketProfileVersion, MarketProfileSource marketProfileSource, Instant generatedAt,
                AiProviderType provider, String model) {
            this(2, analysisProfile, analysisVersion, baselineVersion, marketProfileVersion, marketProfileSource,
                    generatedAt, provider, model);
        }
    }

    public enum CandidateLevel {
        INTERN("intern"),
        JUNIOR("junior"),
        JUNIOR_PLUS("junior_plus"),
        MIDDLE_MINUS("middle_minus"),
        MIDDLE("middle"),
        MIDDLE_PLUS("middle_plus"),
        SENIOR("senior");

        private final String value;

        CandidateLevel(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public enum InterviewChance {
        LOW, MEDIUM, HIGH
    }
}
