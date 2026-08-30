package com.ororura.analyzer.vacancy.requirement;

import com.ororura.analyzer.resume.market.RequirementType;

record CanonicalRequirement(
        String id,
        String label,
        RequirementType type,
        RequirementImportance importance) {
}
