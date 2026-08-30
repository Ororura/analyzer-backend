package com.ororura.analyzer.vacancy.requirement;

import com.ororura.analyzer.resume.market.RequirementType;

public record ExtractedRequirement(
        String label,
        RequirementType type,
        RequirementImportance importance) {

    public ExtractedRequirement {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Extracted requirement label must not be blank");
        }
        if (type == null || importance == null) {
            throw new IllegalArgumentException("Extracted requirement type and importance must be defined");
        }
    }
}
