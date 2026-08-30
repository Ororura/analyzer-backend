package com.ororura.analyzer.resume.ai;

import java.util.List;
import java.util.Map;

public interface ResumeAnalysisProfileDefinition {

    ResumeAnalysisProfile profile();

    String targetRole();

    List<AnalysisCriterion> criteria();

    List<AnalysisTechnology> technologies();

    default Map<String, Double> baselineSkillFrequencies() {
        return Map.of();
    }
}
