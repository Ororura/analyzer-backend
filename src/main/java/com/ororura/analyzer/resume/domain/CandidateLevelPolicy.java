package com.ororura.analyzer.resume.domain;

import org.springframework.stereotype.Component;

import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;

@Component
public class CandidateLevelPolicy {

    public CandidateLevel detect(int months, int technical, int responsibility, int overall) {
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
