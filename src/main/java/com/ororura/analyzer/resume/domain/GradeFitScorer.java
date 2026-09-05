package com.ororura.analyzer.resume.domain;

import java.util.Locale;
import com.ororura.analyzer.resume.api.GradeFitAnalysis;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;
import com.ororura.analyzer.resume.api.ScoreBreakdown;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class GradeFitScorer {
    public GradeFitAnalysis score(CandidateLevel candidate, VacancyMarketData market) {
        CandidateLevel target = target(market);
        if (target == null) {
            return new GradeFitAnalysis(candidate, null, GradeFitAnalysis.GradeFit.UNKNOWN,
                    GradeFitAnalysis.Severity.UNKNOWN, 0, new ScoreBreakdown(null, java.util.List.of()));
        }
        int difference = legacyRank(candidate) - legacyRank(target);
        return comparison(candidate, target, difference);
    }

    public GradeFitAnalysis score(com.ororura.analyzer.analysis.domain.CandidateGrade candidate,
            com.ororura.analyzer.analysis.domain.CandidateGrade target) {
        var policy = new com.ororura.analyzer.analysis.domain.GradePolicy();
        return comparison(candidate.toLegacy(), target.toLegacy(), policy.rank(candidate) - policy.rank(target));
    }

    private static int legacyRank(CandidateLevel value) {
        return switch(value) { case INTERN -> 0; case JUNIOR -> 1; case JUNIOR_PLUS -> 2;
            case MIDDLE_MINUS -> 3; case MIDDLE -> 4; case MIDDLE_PLUS -> 5; case SENIOR -> 6; };
    }

    private GradeFitAnalysis comparison(CandidateLevel candidate, CandidateLevel target, int difference) {
        int distance = Math.abs(difference);
        int score = switch (distance) { case 0 -> 100; case 1 -> 82; case 2 -> 60; default -> 35; };
        var fit = difference == 0 ? GradeFitAnalysis.GradeFit.MATCH
                : difference == 1 ? GradeFitAnalysis.GradeFit.SLIGHTLY_OVERQUALIFIED
                : difference == -1 ? GradeFitAnalysis.GradeFit.SLIGHTLY_UNDERQUALIFIED
                : difference > 0 ? GradeFitAnalysis.GradeFit.OVERQUALIFIED : GradeFitAnalysis.GradeFit.UNDERQUALIFIED;
        var severity = distance == 0 ? GradeFitAnalysis.Severity.NONE
                : distance == 1 ? GradeFitAnalysis.Severity.LOW
                : distance == 2 ? GradeFitAnalysis.Severity.MODERATE : GradeFitAnalysis.Severity.HIGH;
        return new GradeFitAnalysis(candidate, target, fit, severity, score,
                new ScoreBreakdown(score, java.util.List.of(new ScoreBreakdown.ScoreComponent(
                        "levelDistance", score, 1, score, "Разница между подтверждённым и целевым уровнем"))));
    }

    private static CandidateLevel target(VacancyMarketData market) {
        String text = (market.vacancies().stream().map(value -> value.title() == null ? "" : value.title())
                .reduce("", (a, b) -> a + " " + b) + " " + String.join(" ", market.experienceRequirements().keySet()))
                .toLowerCase(Locale.ROOT);
        if (text.contains("senior") || text.contains("сеньор")) return CandidateLevel.SENIOR;
        if (text.contains("middle+") || text.contains("middle plus")) return CandidateLevel.MIDDLE_PLUS;
        if (text.contains("3–6") || text.contains("3-6")) return CandidateLevel.MIDDLE_MINUS;
        if (text.contains("middle") || text.contains("мидл")) {
            return CandidateLevel.MIDDLE;
        }
        if (text.contains("junior+") || text.contains("junior plus")) return CandidateLevel.JUNIOR_PLUS;
        if (text.contains("junior") || text.contains("джун") || text.contains("1–3") || text.contains("1-3")) {
            return CandidateLevel.JUNIOR_PLUS;
        }
        if (text.contains("нет опыта")) return CandidateLevel.JUNIOR;
        if (text.contains("intern") || text.contains("стажер") || text.contains("стажёр")) return CandidateLevel.INTERN;
        return null;
    }
}
