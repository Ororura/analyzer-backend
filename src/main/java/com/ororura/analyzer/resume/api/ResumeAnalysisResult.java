package com.ororura.analyzer.resume.api;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ResumeAnalysisResult")
public record ResumeAnalysisResult(
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
        Market market,
        Metadata metadata,
        List<String> warnings) {

    public ResumeAnalysisResult {
        strengths = List.copyOf(strengths);
        weaknesses = List.copyOf(weaknesses);
        atsIssues = List.copyOf(atsIssues);
        recommendations = List.copyOf(recommendations);
        warnings = List.copyOf(warnings);
    }

    public record Scores(
            int java,
            int spring,
            int backend,
            int sqlPostgresql,
            int hibernateJpa,
            int infrastructure,
            int messagingCache,
            int testing,
            int commercialExperience,
            int experienceDescription,
            int ats,
            int resumeQuality) {
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

    public record Metadata(String analysisVersion, String baselineVersion, Instant generatedAt, String model) {
    }

    public enum CandidateLevel {
        JUNIOR("junior"),
        JUNIOR_PLUS("junior_plus"),
        MIDDLE_MINUS("middle_minus"),
        MIDDLE("middle");

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
