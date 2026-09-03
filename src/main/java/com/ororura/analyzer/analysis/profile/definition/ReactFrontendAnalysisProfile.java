package com.ororura.analyzer.analysis.profile.definition;

import java.util.List;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileDefinition;
public class ReactFrontendAnalysisProfile implements ResumeAnalysisProfileDefinition {

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.REACT_FRONTEND; }
    @Override public String targetRole() { return "React Frontend Developer"; }

    @Override
    public List<String> vacancySearchQueries() {
        return List.of("React Developer", "Frontend Developer React", "React TypeScript Developer",
                "Frontend Developer");
    }
}
