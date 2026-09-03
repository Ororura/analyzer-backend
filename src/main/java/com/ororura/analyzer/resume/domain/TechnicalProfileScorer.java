package com.ororura.analyzer.resume.domain;

import java.util.List;
import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.semantic.CriterionAssessment;
import com.ororura.analyzer.resume.domain.analysis.ScoreBreakdown;
import com.ororura.analyzer.resume.domain.analysis.TechnicalProfile;

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
