package com.ororura.analyzer.market.requirement;

import com.ororura.analyzer.market.domain.RequirementType;

record CanonicalRequirement(
        String id,
        String label,
        RequirementType type,
        RequirementImportance importance) {
}
