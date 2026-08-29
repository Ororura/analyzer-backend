package com.ororura.analyzer.ats.llm.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record LlmAtsAnalysis(
        int hhSearchMatch,
        int recruiterReadability,
        String detectedLevel,
        int targetLevelFit,
        LlmExperience experience,
        LlmStructuredFilters structuredFilters,
        List<LlmTechnologyAssessment> technologies,
        LlmScoreEvidence scoreEvidence,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recruiterRisks,
        List<LlmRecommendation> recommendations,
        String summary) {

    private static final Set<String> DETECTED_LEVELS = Set.of(
            "intern", "junior", "junior_plus", "middle", "middle_plus", "senior");

    public LlmAtsAnalysis {
        requireScore(hhSearchMatch, "hhSearchMatch");
        requireScore(recruiterReadability, "recruiterReadability");
        requireScore(targetLevelFit, "targetLevelFit");
        if (!DETECTED_LEVELS.contains(detectedLevel)) {
            throw new IllegalArgumentException("Unknown detected level: " + detectedLevel);
        }
        technologies = List.copyOf(technologies);
        Set<String> technologyNames = new HashSet<>();
        for (LlmTechnologyAssessment technology : technologies) {
            if (!technologyNames.add(technology.technology().toLowerCase(Locale.forLanguageTag("ru-RU")))) {
                throw new IllegalArgumentException("Технологии не должны дублироваться");
            }
        }
        strengths = List.copyOf(strengths);
        weaknesses = List.copyOf(weaknesses);
        recruiterRisks = List.copyOf(recruiterRisks);
        recommendations = List.copyOf(recommendations);
    }

    public record LlmFilterAssessment(String status, String evidence) {

        private static final Set<String> STATUSES = Set.of("match", "partial", "mismatch", "unknown");

        public LlmFilterAssessment {
            evidence = normalizeEvidence(evidence);
            if (!STATUSES.contains(status)) {
                throw new IllegalArgumentException("Unknown filter status: " + status);
            }
            if (status.equals("unknown") && evidence != null) {
                throw new IllegalArgumentException("unknown должен иметь evidence=null");
            }
            if (!status.equals("unknown") && evidence == null) {
                throw new IllegalArgumentException("Подтверждённый статус требует evidence");
            }
        }
    }

    public record LlmStructuredFilters(
            LlmFilterAssessment experience,
            LlmFilterAssessment education,
            LlmFilterAssessment location,
            LlmFilterAssessment relocation,
            LlmFilterAssessment salary,
            LlmFilterAssessment languages,
            LlmFilterAssessment employmentType,
            LlmFilterAssessment workFormat) {
    }

    public record LlmExperienceAssessment(String value, String evidence) {
        public LlmExperienceAssessment {
            value = normalizeEvidence(value);
            evidence = normalizeEvidence(evidence);
            if (value != null && evidence == null) {
                throw new IllegalArgumentException("Для указанного опыта требуется evidence");
            }
        }
    }

    public record LlmExperience(
            LlmExperienceAssessment totalExperience,
            LlmExperienceAssessment relevantJavaExperience,
            LlmExperienceAssessment backendExperience,
            LlmExperienceAssessment commercialExperience,
            LlmExperienceAssessment projectExperience) {
    }

    public record LlmScoreEvidence(
            List<String> hhSearchMatch,
            List<String> recruiterReadability,
            List<String> targetLevelFit) {
        public LlmScoreEvidence {
            hhSearchMatch = List.copyOf(hhSearchMatch);
            recruiterReadability = List.copyOf(recruiterReadability);
            targetLevelFit = List.copyOf(targetLevelFit);
        }
    }

    public record LlmRecommendation(
            String priority,
            String section,
            String problem,
            String recommendation,
            String example,
            String evidence) {
        public LlmRecommendation {
            evidence = normalizeEvidence(evidence);
        }
    }

    private static String normalizeEvidence(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static void requireScore(int score, String name) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException(name + " must be in range 0..100");
        }
    }
}
