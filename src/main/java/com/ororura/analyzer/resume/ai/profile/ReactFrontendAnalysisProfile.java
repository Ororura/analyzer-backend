package com.ororura.analyzer.resume.ai.profile;

import java.util.List;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileDefinition;
import org.springframework.stereotype.Component;

@Component
public class ReactFrontendAnalysisProfile implements ResumeAnalysisProfileDefinition {

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.REACT_FRONTEND; }
    @Override public String targetRole() { return "React Frontend Developer"; }

    @Override
    public List<String> vacancySearchQueries() {
        return List.of("React Developer", "Frontend Developer React", "React TypeScript Developer",
                "Frontend Developer");
    }
}
