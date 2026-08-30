package com.ororura.analyzer.resume.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.SemanticAssessment;
import org.springframework.stereotype.Component;

@Component
public class OverallScoreCalculator {

    public int technicalScore(List<SemanticAssessment> assessments, List<AnalysisCriterion> criteria) {
        Map<String, SemanticAssessment> byCriterion = assessments.stream()
                .collect(Collectors.toMap(SemanticAssessment::criterion, Function.identity()));
        double totalWeight = criteria.stream().mapToDouble(AnalysisCriterion::weight).sum();
        if (totalWeight <= 0) return 0;
        double weighted = criteria.stream()
                .mapToDouble(criterion -> byCriterion.get(criterion.id()).score() * criterion.weight())
                .sum();
        return ScoreMath.score(weighted / totalWeight * 10);
    }

    public int overall(int technical, int ats, int commercialExperience, int resumeQuality, int responsibility) {
        return ScoreMath.score(technical * .45 + ats * .25 + commercialExperience * 10 * .15
                + resumeQuality * .10 + responsibility * 10 * .05);
    }
}
