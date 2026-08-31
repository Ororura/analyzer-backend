package com.ororura.analyzer.vacancy.requirement;

import java.util.List;
import com.ororura.analyzer.resume.market.RequirementType;

public record ResolvedVacancyRequirements(String vacancyId, List<Requirement> requirements) {
    public ResolvedVacancyRequirements { requirements = List.copyOf(requirements); }
    public record Requirement(String id, String label, RequirementType type, RequirementImportance importance) {}
}
