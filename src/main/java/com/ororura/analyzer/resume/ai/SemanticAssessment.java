package com.ororura.analyzer.resume.ai;

import java.util.List;

public record SemanticAssessment(String criterion, int score, List<String> evidence) {

    public SemanticAssessment {
        if (criterion == null || criterion.isBlank()) {
            throw new IllegalArgumentException("Semantic assessment criterion is required");
        }
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("Semantic score must be in range 0..10");
        }
        if (evidence == null || evidence.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Semantic assessment evidence is required");
        }
        evidence = List.copyOf(evidence);
    }
}
