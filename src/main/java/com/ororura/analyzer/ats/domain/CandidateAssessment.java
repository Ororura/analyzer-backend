package com.ororura.analyzer.ats.domain;

import java.util.Objects;

public record CandidateAssessment(DetectedLevel detectedLevel, CandidateExperience experience) {

    public CandidateAssessment {
        Objects.requireNonNull(detectedLevel, "detectedLevel");
        Objects.requireNonNull(experience, "experience");
    }

    public enum DetectedLevel {
        INTERN, JUNIOR, JUNIOR_PLUS, MIDDLE, MIDDLE_PLUS, SENIOR
    }

    public record CandidateExperience(
            ExperienceAssessment totalExperience,
            ExperienceAssessment relevantJavaExperience,
            ExperienceAssessment backendExperience,
            ExperienceAssessment commercialExperience,
            ExperienceAssessment projectExperience) {
    }

    public record ExperienceAssessment(String value, String evidence) {
        public ExperienceAssessment {
            value = normalizeEvidence(value);
            evidence = normalizeEvidence(evidence);
            if (value != null && evidence == null) {
                throw new IllegalArgumentException("Для указанного опыта требуется evidence");
            }
        }
    }

    private static String normalizeEvidence(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
