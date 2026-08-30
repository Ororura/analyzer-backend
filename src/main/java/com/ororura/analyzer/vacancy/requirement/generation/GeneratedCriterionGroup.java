package com.ororura.analyzer.vacancy.requirement.generation;

import java.util.List;

public record GeneratedCriterionGroup(
        String label,
        String description,
        List<String> requirementIds) {

    public GeneratedCriterionGroup {
        if (label == null || label.isBlank() || description == null || description.isBlank()
                || requirementIds == null || requirementIds.isEmpty()) {
            throw new IllegalArgumentException("Generated criterion group is incomplete");
        }
        requirementIds = List.copyOf(requirementIds);
    }
}
