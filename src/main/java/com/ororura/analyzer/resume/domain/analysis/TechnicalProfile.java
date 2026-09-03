package com.ororura.analyzer.resume.domain.analysis;

import java.util.List;
import com.ororura.analyzer.analysis.semantic.CriterionAssessment;

public record TechnicalProfile(int score, List<CriterionAssessment> assessments, ScoreBreakdown breakdown) {
    public TechnicalProfile { assessments = List.copyOf(assessments); }
}
