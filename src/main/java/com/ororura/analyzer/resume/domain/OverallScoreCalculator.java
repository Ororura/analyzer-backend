package com.ororura.analyzer.resume.domain;

import org.springframework.stereotype.Component;

@Component
public class OverallScoreCalculator {

    public int technicalScore(int java, int spring, int backend, int sql, int jpa,
            int infrastructure, int messagingCache, int testing) {
        return ScoreMath.score((java * .20 + spring * .15 + backend * .15 + sql * .10
                + jpa * .10 + infrastructure * .10 + messagingCache * .10 + testing * .10) * 10);
    }

    public int overall(int technical, int ats, int commercialExperience, int resumeQuality, int responsibility) {
        return ScoreMath.score(technical * .45 + ats * .25 + commercialExperience * 10 * .15
                + resumeQuality * .10 + responsibility * 10 * .05);
    }
}
