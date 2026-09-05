package com.ororura.analyzer.analysis.domain;

import java.util.List;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

/** Defaults are copied on creation. A preset never owns or mutates a user profile. */
public record AnalysisPreset(ResumeAnalysisProfile id, CareerDirection direction, String specialization,
        CandidateGrade targetGrade, List<String> technologies) {
    public static AnalysisPreset legacy(ResumeAnalysisProfile id) {
        return switch (id) {
            case JAVA_BACKEND -> new AnalysisPreset(id, CareerDirection.BACKEND, "Java", CandidateGrade.JUNIOR,
                    List.of("Java", "Spring Boot"));
            case REACT_FRONTEND -> new AnalysisPreset(id, CareerDirection.FRONTEND, "React", CandidateGrade.JUNIOR,
                    List.of("React", "TypeScript"));
        };
    }
}
