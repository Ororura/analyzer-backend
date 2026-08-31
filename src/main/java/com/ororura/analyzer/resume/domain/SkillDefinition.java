package com.ororura.analyzer.resume.domain;

import java.util.List;

public record SkillDefinition(String id, String displayName, List<String> aliases, String parentSkill) {
    public SkillDefinition {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Skill id and display name are required");
        }
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }
}
