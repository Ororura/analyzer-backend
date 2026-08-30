package com.ororura.analyzer.resume.ai;

import java.util.List;

public record CriterionAssessment(String criterionId, int score, List<String> evidence) {

    public CriterionAssessment {
        if (criterionId == null || criterionId.isBlank()) {
            throw new IllegalArgumentException("Criterion assessment id is required");
        }
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("Criterion assessment score must be in range 0..10");
        }
        if (evidence == null || evidence.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Criterion assessment evidence is required");
        }
        evidence = List.copyOf(evidence);
    }
}
