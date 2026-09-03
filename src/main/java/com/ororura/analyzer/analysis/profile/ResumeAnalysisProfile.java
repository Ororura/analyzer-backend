package com.ororura.analyzer.analysis.profile;

public enum ResumeAnalysisProfile {
    JAVA_BACKEND,
    REACT_FRONTEND;

    public static ResumeAnalysisProfile defaultProfile() {
        return JAVA_BACKEND;
    }
}
