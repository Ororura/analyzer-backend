package com.ororura.analyzer.resume.domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.CriterionAssessment;
import org.springframework.stereotype.Component;

@Component
public class OverallScoreCalculator {

    private static final double NORMALIZED_WEIGHT_EPSILON = 1e-9;

    public int technicalScore(List<CriterionAssessment> assessments, List<AnalysisCriterion> criteria) {
        Map<String, CriterionAssessment> byCriterion = assessments.stream()
                .collect(Collectors.toMap(CriterionAssessment::criterionId, Function.identity()));
        double totalWeight = criteria.stream().mapToDouble(AnalysisCriterion::weight).sum();
        if (Math.abs(totalWeight - 1.0) > NORMALIZED_WEIGHT_EPSILON) {
            throw new IllegalArgumentException("Market criterion weights must be normalized");
        }
        if (byCriterion.size() != criteria.size()
                || criteria.stream().anyMatch(criterion -> !byCriterion.containsKey(criterion.id()))) {
            throw new IllegalArgumentException("Criterion assessments must match market profile criteria");
        }
        double technicalScore = criteria.stream()
                .mapToDouble(criterion -> byCriterion.get(criterion.id()).score() * criterion.weight())
                .sum();
        return ScoreMath.score(technicalScore * 10);
    }

    public int overall(int technical, int ats, int commercialExperience, int resumeQuality, int responsibility) {
        return ScoreMath.score(technical * .45 + ats * .25 + commercialExperience * 10 * .15
                + resumeQuality * .10 + responsibility * 10 * .05);
    }
}
