package com.ororura.analyzer.resume.domain;

import org.springframework.stereotype.Component;

import com.ororura.analyzer.resume.api.ResumeAnalysisResult.InterviewChance;

@Component
public class InterviewChancePolicy {

    public InterviewChance hr(int ats, int overall) {
        if (ats >= 75 && overall >= 65) return InterviewChance.HIGH;
        if (ats >= 50 && overall >= 45) return InterviewChance.MEDIUM;
        return InterviewChance.LOW;
    }

    public InterviewChance technical(int technical, int overall) {
        if (technical >= 75 && overall >= 70) return InterviewChance.HIGH;
        if (technical >= 55 && overall >= 50) return InterviewChance.MEDIUM;
        return InterviewChance.LOW;
    }
}
