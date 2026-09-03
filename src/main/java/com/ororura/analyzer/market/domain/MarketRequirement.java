package com.ororura.analyzer.market.domain;

public record MarketRequirement(
        String id,
        String label,
        RequirementType type,
        double frequency) {

    public MarketRequirement {
        if (id == null || id.isBlank() || label == null || label.isBlank()) {
            throw new IllegalArgumentException("Market requirement id and label must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Market requirement type must be defined");
        }
        if (!Double.isFinite(frequency) || frequency < 0 || frequency > 1) {
            throw new IllegalArgumentException("Market requirement frequency must be between 0 and 1");
        }
    }
}
