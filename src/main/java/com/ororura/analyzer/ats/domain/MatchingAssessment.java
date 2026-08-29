package com.ororura.analyzer.ats.domain;

import java.util.List;

public record MatchingAssessment(
        ResumeFilterAssessment filters,
        List<TechnologyAssessment> technologies,
        Keywords keywords) {

    public MatchingAssessment {
        technologies = List.copyOf(technologies);
    }

    public enum FilterStatus {
        MATCH, PARTIAL, MISMATCH, UNKNOWN
    }

    public enum TechnologyStatus {
        CONFIRMED_EXPERIENCE, SEMANTIC_EXPERIENCE, EXPLICIT_OTHER, SKILLS_ONLY, MISSING, IRRELEVANT
    }

    public enum TechnologyTier {
        CORE, COMMON, BONUS
    }

    public record FilterAssessment(FilterStatus status, String evidence) {
        public FilterAssessment {
            evidence = normalizeEvidence(evidence);
            if (status == FilterStatus.UNKNOWN && evidence != null) {
                throw new IllegalArgumentException("unknown должен иметь evidence=null");
            }
            if (status != FilterStatus.UNKNOWN && evidence == null) {
                throw new IllegalArgumentException("Подтверждённый статус требует evidence");
            }
        }
    }

    public record ResumeFilterAssessment(
            FilterAssessment experience,
            FilterAssessment education,
            FilterAssessment location,
            FilterAssessment relocation,
            FilterAssessment salary,
            FilterAssessment languages,
            FilterAssessment employmentType,
            FilterAssessment workFormat) {
    }

    public record TechnologyAssessment(
            String technology,
            TechnologyStatus status,
            String evidence,
            TechnologyTier tier) {
    }

    public record Keywords(
            List<String> explicitlyPresent,
            List<String> semanticallyPresent,
            List<String> confirmedByExperience,
            List<String> skillsOnly,
            List<String> missingCore,
            List<String> missingCommon,
            List<String> optional,
            List<String> irrelevantKeywords) {
        public Keywords {
            explicitlyPresent = List.copyOf(explicitlyPresent);
            semanticallyPresent = List.copyOf(semanticallyPresent);
            confirmedByExperience = List.copyOf(confirmedByExperience);
            skillsOnly = List.copyOf(skillsOnly);
            missingCore = List.copyOf(missingCore);
            missingCommon = List.copyOf(missingCommon);
            optional = List.copyOf(optional);
            irrelevantKeywords = List.copyOf(irrelevantKeywords);
        }
    }

    private static String normalizeEvidence(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
