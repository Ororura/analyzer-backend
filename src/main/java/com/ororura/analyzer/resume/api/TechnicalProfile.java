package com.ororura.analyzer.resume.api;

import java.util.List;
import com.ororura.analyzer.resume.ai.CriterionAssessment;

public record TechnicalProfile(int score, List<CriterionAssessment> assessments, ScoreBreakdown breakdown) {
    public TechnicalProfile { assessments = List.copyOf(assessments); }
}
