package com.ororura.analyzer.analysis.domain;

import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;

public enum CandidateGrade {
    INTERN, JUNIOR, JUNIOR_PLUS, MIDDLE, MIDDLE_PLUS, SENIOR;

    /** Compatibility only: the former MIDDLE_MINUS is conservatively mapped to JUNIOR_PLUS. */
    public static CandidateGrade fromLegacy(CandidateLevel value) {
        if (value == null) return null;
        return switch (value) {
            case INTERN -> INTERN; case JUNIOR -> JUNIOR;
            case JUNIOR_PLUS, MIDDLE_MINUS -> JUNIOR_PLUS;
            case MIDDLE -> MIDDLE; case MIDDLE_PLUS -> MIDDLE_PLUS; case SENIOR -> SENIOR;
        };
    }
    public CandidateLevel toLegacy() { return CandidateLevel.valueOf(name()); }
}
