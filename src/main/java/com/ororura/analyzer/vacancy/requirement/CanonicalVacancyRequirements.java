package com.ororura.analyzer.vacancy.requirement;

import java.util.List;

record CanonicalVacancyRequirements(String vacancyId, List<CanonicalRequirement> requirements) {

    CanonicalVacancyRequirements {
        requirements = List.copyOf(requirements);
    }
}
