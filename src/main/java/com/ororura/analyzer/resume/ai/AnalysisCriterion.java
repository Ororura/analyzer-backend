package com.ororura.analyzer.resume.ai;

public record AnalysisCriterion(
        String id,
        String label,
        String description,
        double weight,
        double marketFrequency) {

    public AnalysisCriterion(String id, String label, String description, double weight) {
        this(id, label, description, weight, 0);
    }

    public AnalysisCriterion {
        if (id == null || id.isBlank() || label == null || label.isBlank()
                || description == null || description.isBlank()) {
            throw new IllegalArgumentException("Analysis criterion fields must not be blank");
        }
        if (!Double.isFinite(weight) || weight < 0) {
            throw new IllegalArgumentException("Analysis criterion weight must be finite and non-negative");
        }
        if (!Double.isFinite(marketFrequency) || marketFrequency < 0 || marketFrequency > 1) {
            throw new IllegalArgumentException("Analysis criterion market frequency must be between 0 and 1");
        }
    }
}
