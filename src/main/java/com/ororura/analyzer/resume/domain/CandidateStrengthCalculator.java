package com.ororura.analyzer.resume.domain;

import org.springframework.stereotype.Component;

@Component
public class CandidateStrengthCalculator {

    public int calculate(int technical, int commercialExperience, int responsibility) {
        return ScoreMath.score(technical * .55 + commercialExperience * 10 * .25 + responsibility * 10 * .20);
    }
}
