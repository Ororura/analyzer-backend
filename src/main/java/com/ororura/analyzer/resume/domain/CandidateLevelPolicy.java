package com.ororura.analyzer.resume.domain;

import org.springframework.stereotype.Component;

import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;

@Component
public class CandidateLevelPolicy {

    public CandidateLevel detect(int months, int technical, int responsibility, int overall) {
        if (months >= 72 && technical >= 90 && responsibility >= 9 && overall >= 88) {
            return CandidateLevel.SENIOR;
        }
        if (months >= 48 && technical >= 82 && responsibility >= 8 && overall >= 82) {
            return CandidateLevel.MIDDLE_PLUS;
        }
        if (months >= 36 && technical >= 75 && responsibility >= 7 && overall >= 75) {
            return CandidateLevel.MIDDLE;
        }
        if (months >= 24 && technical >= 65 && responsibility >= 6 && overall >= 65) {
            return CandidateLevel.MIDDLE_MINUS;
        }
        if (months >= 12 && technical >= 55 && overall >= 55) {
            return CandidateLevel.JUNIOR_PLUS;
        }
        return CandidateLevel.JUNIOR;
    }
}
