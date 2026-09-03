package com.ororura.analyzer.market.requirement;

import java.util.List;

public record ExtractedVacancyRequirements(
        String vacancyId,
        List<ExtractedRequirement> requirements) {

    public ExtractedVacancyRequirements {
        if (vacancyId == null || vacancyId.isBlank()) {
            throw new IllegalArgumentException("Extracted vacancy id must not be blank");
        }
        if (requirements == null) {
            throw new IllegalArgumentException("Extracted vacancy requirements must be defined");
        }
        requirements = List.copyOf(requirements);
    }
}
