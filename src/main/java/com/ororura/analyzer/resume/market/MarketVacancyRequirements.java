package com.ororura.analyzer.resume.market;

import java.util.List;

import com.ororura.analyzer.vacancy.requirement.RequirementImportance;

public record MarketVacancyRequirements(String vacancyId, List<Requirement> requirements) {
    public MarketVacancyRequirements {
        if (vacancyId == null || vacancyId.isBlank()) {
            throw new IllegalArgumentException("Vacancy id is required");
        }
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
    }

    public record Requirement(String requirementId, RequirementImportance importance) {
        public Requirement {
            if (requirementId == null || requirementId.isBlank() || importance == null) {
                throw new IllegalArgumentException("Vacancy requirement is invalid");
            }
        }
    }
}
