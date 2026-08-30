package com.ororura.analyzer.resume.ai.profile;

import java.util.List;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileDefinition;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class JavaBackendAnalysisProfile implements ResumeAnalysisProfileDefinition {

    private final String targetRole;

    @Autowired
    public JavaBackendAnalysisProfile(ResumeAnalysisProperties properties) {
        this.targetRole = properties.targetRole();
    }

    public JavaBackendAnalysisProfile() {
        this.targetRole = "Java Backend Developer";
    }

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.JAVA_BACKEND; }
    @Override public String targetRole() { return targetRole; }

    @Override
    public List<String> vacancySearchQueries() {
        return List.of("Java Backend Developer", "Java Developer", "Java Spring Developer");
    }
}
