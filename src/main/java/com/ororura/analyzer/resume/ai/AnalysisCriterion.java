package com.ororura.analyzer.resume.ai;

public record AnalysisCriterion(String id, String label, String description, double weight) {

    public AnalysisCriterion {
        if (id == null || id.isBlank() || label == null || label.isBlank()
                || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Analysis criterion fields must not be blank");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Analysis criterion weight must be positive");
        }
    }
}
