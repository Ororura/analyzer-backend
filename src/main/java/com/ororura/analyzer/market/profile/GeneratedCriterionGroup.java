package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

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
