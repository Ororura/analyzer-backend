package com.ororura.analyzer.ats.domain;

import java.util.List;

import com.ororura.analyzer.ats.domain.AtsScores.ScoreAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.ResumeFilterAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyStatus;

public record NormalizedAtsAnalysis(
        CandidateAssessment candidate,
        ResumeFilterAssessment filters,
        List<NormalizedTechnologyAssessment> technologies,
        ScoreAssessment hhSearchMatch,
        ScoreAssessment recruiterReadability,
        ScoreAssessment targetLevelFit,
        AnalysisInsights insights) {

    public NormalizedAtsAnalysis {
        technologies = List.copyOf(technologies);
    }

    public record NormalizedTechnologyAssessment(
            String technology,
            TechnologyStatus status,
            String evidence) {
        public NormalizedTechnologyAssessment {
            technology = technology == null ? "" : technology.trim();
            evidence = evidence == null || evidence.trim().isEmpty() ? null : evidence.trim();
            if (technology.isEmpty()) {
                throw new IllegalArgumentException("technology must not be blank");
            }
        }
    }
}
