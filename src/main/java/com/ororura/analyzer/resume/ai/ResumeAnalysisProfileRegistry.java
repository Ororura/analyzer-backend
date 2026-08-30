package com.ororura.analyzer.resume.ai;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisProfileRegistry {

    private final Map<ResumeAnalysisProfile, LegacyResumeAnalysisProfileDefinition> profiles;

    public ResumeAnalysisProfileRegistry(List<LegacyResumeAnalysisProfileDefinition> definitions) {
        EnumMap<ResumeAnalysisProfile, LegacyResumeAnalysisProfileDefinition> resolved =
                new EnumMap<>(ResumeAnalysisProfile.class);
        for (LegacyResumeAnalysisProfileDefinition definition : definitions) {
            LegacyResumeAnalysisProfileDefinition previous = resolved.putIfAbsent(definition.profile(), definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate resume analysis profile: " + definition.profile());
            }
        }
        for (ResumeAnalysisProfile profile : ResumeAnalysisProfile.values()) {
            if (!resolved.containsKey(profile)) {
                throw new IllegalStateException("Missing resume analysis profile definition: " + profile);
            }
        }
        this.profiles = Map.copyOf(resolved);
    }

    public LegacyResumeAnalysisProfileDefinition get(ResumeAnalysisProfile profile) {
        LegacyResumeAnalysisProfileDefinition definition = profiles.get(profile);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown resume analysis profile: " + profile);
        }
        return definition;
    }
}
