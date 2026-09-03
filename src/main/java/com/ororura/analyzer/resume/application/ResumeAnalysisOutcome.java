package com.ororura.analyzer.resume.application;

import java.time.Instant;
import java.util.List;

import com.ororura.analyzer.market.domain.MarketProfileSource;
import com.ororura.analyzer.resume.application.port.AiProviderType;
import com.ororura.analyzer.analysis.semantic.CriterionAssessment;
import com.ororura.analyzer.resume.domain.analysis.*;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;

public record ResumeAnalysisOutcome(
        String targetRole,
        CandidateLevel detectedLevel,
        Scores scores,
        int overallScore,
        int candidateStrength,
        InterviewChance hrScreeningChance,
        InterviewChance technicalInterviewChance,
        Experience experience,
        Skills skills,
        List<String> strengths,
        List<String> weaknesses,
        List<String> atsIssues,
        List<String> recommendations,
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
        List<String> warnings) {

    public ResumeAnalysisOutcome {
        strengths = List.copyOf(strengths);
        weaknesses = List.copyOf(weaknesses);
        atsIssues = List.copyOf(atsIssues);
        recommendations = List.copyOf(recommendations);
        risks = List.copyOf(risks);
        warnings = List.copyOf(warnings);
    }

    public record Scores(List<CriterionAssessment> assessments, int commercialExperience,
            int experienceDescription, int ats, int resumeQuality) {
        public Scores { assessments = List.copyOf(assessments); }
    }

    public record Experience(int commercialMonths, int commercialYears, int remainingMonths) {}

    public record Skills(List<String> confirmed, List<String> weakEvidence, List<String> missing) {
        public Skills {
            confirmed = List.copyOf(confirmed);
            weakEvidence = List.copyOf(weakEvidence);
            missing = List.copyOf(missing);
        }
    }

    public record Market(String source, int sampleSize) {}

    public record Metadata(int analysisSchemaVersion, ResumeAnalysisProfile analysisProfile, String analysisVersion,
            String baselineVersion, String marketProfileVersion, MarketProfileSource marketProfileSource,
            Instant generatedAt, AiProviderType provider, String model) {}
}
