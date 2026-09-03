package com.ororura.analyzer.analysis.profile.definition;

import java.util.List;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileDefinition;
public class JavaBackendAnalysisProfile implements ResumeAnalysisProfileDefinition {

    private final String targetRole;

    public JavaBackendAnalysisProfile(String targetRole) {
        if (targetRole == null || targetRole.isBlank()) {
            throw new IllegalArgumentException("Java backend target role must not be blank");
        }
        this.targetRole = targetRole;
    }

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.JAVA_BACKEND; }
    @Override public String targetRole() { return targetRole; }

    @Override
    public List<String> vacancySearchQueries() {
        return List.of("Java Backend Developer", "Java Developer", "Java Spring Developer");
    }
}
