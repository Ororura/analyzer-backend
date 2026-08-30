package com.ororura.analyzer.vacancy.requirement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import org.springframework.stereotype.Component;

@Component
class VacancyRequirementMerger {

    private final RequirementCanonicalizer canonicalizer;

    VacancyRequirementMerger(RequirementCanonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    CanonicalVacancyRequirements merge(ResumeAnalysisProfile profile, String vacancyId,
            List<ExtractedRequirement> semantic, List<ExtractedRequirement> deterministic) {
        Map<String, CanonicalRequirement> merged = new LinkedHashMap<>();
        for (ExtractedRequirement requirement : semantic) {
            CanonicalRequirement canonical = canonicalizer.canonicalize(profile, requirement);
            merged.merge(canonical.id(), canonical, VacancyRequirementMerger::stronger);
        }
        for (ExtractedRequirement requirement : deterministic) {
            CanonicalRequirement canonical = canonicalizer.canonicalize(profile, requirement);
            merged.putIfAbsent(canonical.id(), canonical);
        }
        return new CanonicalVacancyRequirements(vacancyId, List.copyOf(merged.values()));
    }

    private static CanonicalRequirement stronger(CanonicalRequirement first, CanonicalRequirement second) {
        return rank(second.importance()) > rank(first.importance()) ? second : first;
    }

    private static int rank(RequirementImportance importance) {
        return switch (importance) {
            case REQUIRED -> 3;
            case PREFERRED -> 2;
            case OPTIONAL -> 1;
        };
    }
}
