package com.ororura.analyzer.analysis.profile;

import java.util.List;

public interface ResumeAnalysisProfileDefinition {

    ResumeAnalysisProfile profile();

    String targetRole();

    default List<String> vacancySearchQueries() {
        return List.of(targetRole());
    }
}
