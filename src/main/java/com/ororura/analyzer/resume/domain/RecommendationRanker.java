package com.ororura.analyzer.resume.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.ororura.analyzer.resume.api.AtsAnalysis;
import com.ororura.analyzer.resume.api.ResumeClaimRiskAnalysis;
import com.ororura.analyzer.resume.api.ResumeRecommendationAnalysis;
import com.ororura.analyzer.resume.api.SkillGapAnalysis;
import com.ororura.analyzer.resume.api.SkillRoiAnalysis;
import org.springframework.stereotype.Component;

@Component
public class RecommendationRanker {
    public ResumeRecommendationAnalysis build(AtsAnalysis ats, SkillGapAnalysis gaps,
            ResumeClaimRiskAnalysis claims, List<String> legacy) {
        List<ResumeRecommendationAnalysis.Recommendation> result = new ArrayList<>();
        ats.diagnostics().stream().filter(value -> value.status() != AtsAnalysis.FieldStatus.OK).forEach(value ->
                result.add(new ResumeRecommendationAnalysis.Recommendation(
                        "Исправить ATS-поле: " + value.field(), ResumeRecommendationAnalysis.Category.ATS,
                        value.status() == AtsAnalysis.FieldStatus.FAILED
                                ? ResumeRecommendationAnalysis.Priority.HIGH : ResumeRecommendationAnalysis.Priority.MEDIUM,
                        SkillRoiAnalysis.Effort.LOW, Map.of("atsScore", value.status() == AtsAnalysis.FieldStatus.FAILED ? 8 : 4),
                        String.join("; ", value.issues()))));
        gaps.gaps().stream().limit(8).forEach(value -> result.add(new ResumeRecommendationAnalysis.Recommendation(
                "Усилить подтверждение навыка " + value.skill(), ResumeRecommendationAnalysis.Category.SKILL,
                switch (value.priority()) { case HIGH -> ResumeRecommendationAnalysis.Priority.HIGH;
                    case MEDIUM -> ResumeRecommendationAnalysis.Priority.MEDIUM;
                    case LOW -> ResumeRecommendationAnalysis.Priority.LOW; },
                SkillRoiAnalysis.Effort.HIGH, Map.of("marketCoverage", (int) Math.round(value.estimatedCoverageGain() * 100)),
                "Навык встречается в " + Math.round(value.marketFrequency() * 100) + "% вакансий")));
        claims.claims().stream().filter(value -> value.recommendation()
                != ResumeClaimRiskAnalysis.ClaimRecommendation.KEEP).forEach(value -> result.add(
                new ResumeRecommendationAnalysis.Recommendation("Подготовить доказательства claim",
                        ResumeRecommendationAnalysis.Category.CLAIM,
                        value.interviewRisk() >= 7 ? ResumeRecommendationAnalysis.Priority.HIGH
                                : ResumeRecommendationAnalysis.Priority.MEDIUM,
                        SkillRoiAnalysis.Effort.LOW, Map.of("interviewRisk", value.interviewRisk()), value.claim())));
        if (result.isEmpty()) legacy.stream().limit(5).forEach(value -> result.add(
                new ResumeRecommendationAnalysis.Recommendation(value,
                        ResumeRecommendationAnalysis.Category.EXPERIENCE,
                        ResumeRecommendationAnalysis.Priority.MEDIUM, SkillRoiAnalysis.Effort.LOW,
                        Map.of(), value)));
        result.sort(Comparator.comparing(ResumeRecommendationAnalysis.Recommendation::priority)
                .thenComparing(Comparator.comparingInt(RecommendationRanker::impact).reversed())
                .thenComparing(ResumeRecommendationAnalysis.Recommendation::effort));
        return new ResumeRecommendationAnalysis(result);
    }

    private static int impact(ResumeRecommendationAnalysis.Recommendation value) {
        return value.expectedImpact().values().stream().mapToInt(Integer::intValue).sum();
    }
}
