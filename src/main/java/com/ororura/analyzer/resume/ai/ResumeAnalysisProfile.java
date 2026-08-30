package com.ororura.analyzer.resume.ai;

public enum ResumeAnalysisProfile {
    JAVA_BACKEND,
    REACT_FRONTEND;

    public static ResumeAnalysisProfile defaultProfile() {
        return JAVA_BACKEND;
    }
}
