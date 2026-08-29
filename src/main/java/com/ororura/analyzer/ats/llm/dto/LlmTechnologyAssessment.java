package com.ororura.analyzer.ats.llm.dto;

import java.util.Set;

public record LlmTechnologyAssessment(String technology, String status, String evidence) {

    private static final Set<String> STATUSES = Set.of(
            "confirmed_experience", "semantic_experience", "explicit_other",
            "skills_only", "missing", "irrelevant");

    public LlmTechnologyAssessment {
        technology = technology == null ? "" : technology.trim();
        evidence = normalizeEvidence(evidence);
        if (technology.isEmpty()) {
            throw new IllegalArgumentException("technology must not be blank");
        }
        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unknown technology status: " + status);
        }
        if (status.equals("missing") && evidence != null) {
            throw new IllegalArgumentException("missing должен иметь evidence=null");
        }
        if (Set.of("confirmed_experience", "semantic_experience", "explicit_other").contains(status)
                && evidence == null) {
            throw new IllegalArgumentException("Присутствующая технология требует evidence");
        }
    }

    private static String normalizeEvidence(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
