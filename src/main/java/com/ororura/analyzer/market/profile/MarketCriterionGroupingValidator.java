package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.ororura.analyzer.market.requirement.MarketRequirementStatistics;
import org.springframework.stereotype.Component;

@Component
class MarketCriterionGroupingValidator {

    private static final Set<String> UNIVERSAL_ASSESSMENTS = Set.of(
            "resumequality", "atsreadability", "experiencedescriptionquality",
            "responsibilitylevel", "commercialrelevance");
    private final MarketCriterionProperties properties;

    MarketCriterionGroupingValidator(MarketCriterionProperties properties) {
        this.properties = properties;
    }

    List<GeneratedCriterionGroup> validate(List<GeneratedCriterionGroup> groups,
            MarketRequirementStatistics statistics) {
        if (groups.size() < properties.minimumCount() || groups.size() > properties.maximumCount()) {
            throw invalid("Criterion count is outside the configured range");
        }
        Set<String> knownIds = statistics.requirements().stream()
                .map(MarketRequirementStatistics.RequirementStatistics::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> labels = new HashSet<>();
        Set<String> assigned = new HashSet<>();
        for (GeneratedCriterionGroup group : groups) {
            String normalizedLabel = normalize(group.label());
            if (!labels.add(normalizedLabel)) {
                throw invalid("Duplicate criterion label: " + group.label());
            }
            if (UNIVERSAL_ASSESSMENTS.contains(normalizedLabel.replaceAll("[^a-z]", ""))) {
                throw invalid("Universal resume assessment cannot be market-generated: " + group.label());
            }
            Set<String> local = new HashSet<>();
            for (String requirementId : group.requirementIds()) {
                if (requirementId == null || requirementId.isBlank() || !knownIds.contains(requirementId)) {
                    throw invalid("Unknown requirement id: " + requirementId);
                }
                if (!local.add(requirementId)) {
                    throw invalid("Duplicate requirement id inside criterion: " + requirementId);
                }
                if (!assigned.add(requirementId)) {
                    throw invalid("Requirement is assigned to more than one criterion: " + requirementId);
                }
            }
        }
        return List.copyOf(groups);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static MarketCriterionGenerationException invalid(String message) {
        return new MarketCriterionGenerationException(message);
    }
}
