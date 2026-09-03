package com.ororura.analyzer.market.requirement;

import java.util.List;
import com.ororura.analyzer.market.domain.RequirementType;

public record ResolvedVacancyRequirements(String vacancyId, List<Requirement> requirements) {
    public ResolvedVacancyRequirements { requirements = List.copyOf(requirements); }
    public record Requirement(String id, String label, RequirementType type, RequirementImportance importance) {}
}
