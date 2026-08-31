package com.ororura.analyzer.resume.domain;

import java.util.List;
import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.CriterionAssessment;
import com.ororura.analyzer.resume.api.ScoreBreakdown;
import com.ororura.analyzer.resume.api.TechnicalProfile;
import org.springframework.stereotype.Component;

@Component
public class TechnicalProfileScorer {
    private final OverallScoreCalculator calculator;
    public TechnicalProfileScorer(OverallScoreCalculator calculator) { this.calculator = calculator; }
    public TechnicalProfile score(List<CriterionAssessment> values, List<AnalysisCriterion> criteria) {
        int score = calculator.technicalScore(values, criteria);
        var byId = values.stream().collect(java.util.stream.Collectors.toMap(CriterionAssessment::criterionId, v -> v));
        var components = criteria.stream().map(criterion -> new ScoreBreakdown.ScoreComponent(criterion.label(),
                byId.get(criterion.id()).score() * 10, criterion.weight(),
                byId.get(criterion.id()).score() * 10 * criterion.weight(),
                String.join("; ", byId.get(criterion.id()).evidence()))).toList();
        return new TechnicalProfile(score, values, new ScoreBreakdown(score, components));
    }
}
