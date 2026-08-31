package com.ororura.analyzer.resume.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.ororura.analyzer.resume.api.GradeFitAnalysis;
import com.ororura.analyzer.resume.api.ScoreBreakdown;
import com.ororura.analyzer.resume.api.SkillEvidenceAnalysis;
import com.ororura.analyzer.resume.api.VacancyFitAnalysis;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class VacancyFitScorer {
    public VacancyFitAnalysis score(VacancyMarketData market, SkillEvidenceAnalysis evidence,
            int commercialMonths, GradeFitAnalysis grade, int technicalScore) {
        Map<String, SkillEvidenceAnalysis.SkillEvidence> bySkill = evidence.skills().stream()
                .collect(java.util.stream.Collectors.toMap(value -> value.skill().toLowerCase(), value -> value));
        var must = market.skillStatistics().stream().filter(value -> value.requiredFrequency() > 0).toList();
        var nice = market.skillStatistics().stream().filter(value -> value.requiredFrequency() == 0).toList();
        int mustCoverage = coverage(must, bySkill);
        int niceCoverage = coverage(nice, bySkill);
        Integer experienceFit = experienceFit(market, commercialMonths);
        List<VacancyFitAnalysis.Blocker> blockers = blockers(market, commercialMonths, bySkill);
        double weight = .35 + .15 + .15 + .15 + (experienceFit == null ? 0 : .20);
        double value = mustCoverage * .35 + niceCoverage * .15 + grade.score() * .15 + technicalScore * .15
                + (experienceFit == null ? 0 : experienceFit * .20);
        int score = (int) Math.clamp(Math.round(value / weight), 0, 100);
        var recommendation = recommendation(mustCoverage, grade.score(), technicalScore, blockers);
        var components = new ArrayList<ScoreBreakdown.ScoreComponent>();
        components.add(component("mustHaveCoverage", mustCoverage, .35 / weight));
        components.add(component("niceToHaveCoverage", niceCoverage, .15 / weight));
        if (experienceFit != null) components.add(component("experienceFit", experienceFit, .20 / weight));
        components.add(component("gradeFit", grade.score(), .15 / weight));
        components.add(component("technicalFit", technicalScore, .15 / weight));
        return new VacancyFitAnalysis(score, mustCoverage, niceCoverage, experienceFit, grade.score(), technicalScore,
                recommendation, blockers, new ScoreBreakdown(score, components));
    }

    private static int coverage(List<com.ororura.analyzer.resume.market.MarketSkillStatistics> stats,
            Map<String, SkillEvidenceAnalysis.SkillEvidence> evidence) {
        double total = stats.stream().mapToDouble(value -> value.frequency()).sum();
        if (total == 0) return 100;
        double earned = stats.stream().mapToDouble(value -> value.frequency() * EvidenceClassifier.weight(
                evidence.getOrDefault(value.displayName().toLowerCase(), missing()).status())).sum();
        return (int) Math.clamp(Math.round(earned / total * 100), 0, 100);
    }

    private static SkillEvidenceAnalysis.SkillEvidence missing() {
        return new SkillEvidenceAnalysis.SkillEvidence("", "", SkillEvidenceAnalysis.EvidenceStatus.NOT_FOUND, 0, List.of());
    }

    private static Integer experienceFit(VacancyMarketData market, int months) {
        int minimum = minimumMonths(market);
        if (minimum == 0) return null;
        return (int) Math.clamp(Math.round(Math.min(1, (double) months / minimum) * 100), 0, 100);
    }

    private static List<VacancyFitAnalysis.Blocker> blockers(VacancyMarketData market, int months,
            Map<String, SkillEvidenceAnalysis.SkillEvidence> evidence) {
        List<VacancyFitAnalysis.Blocker> result = new ArrayList<>();
        int minimum = minimumMonths(market);
        if (minimum > 0 && months < minimum) result.add(new VacancyFitAnalysis.Blocker(
                VacancyFitAnalysis.BlockerType.EXPERIENCE, minimum / 12 + "+ years", months + " months",
                VacancyFitAnalysis.BlockerSeverity.HIGH));
        market.skillStatistics().stream().filter(value -> value.requiredFrequency() >= 1).forEach(value -> {
            var status = evidence.getOrDefault(value.displayName().toLowerCase(), missing()).status();
            if (status == SkillEvidenceAnalysis.EvidenceStatus.NOT_FOUND) {
                result.add(new VacancyFitAnalysis.Blocker(VacancyFitAnalysis.BlockerType.SKILL,
                        value.displayName(), "NOT_FOUND", VacancyFitAnalysis.BlockerSeverity.HIGH));
            }
        });
        return List.copyOf(result);
    }

    private static int minimumMonths(VacancyMarketData market) {
        String value = String.join(" ", market.experienceRequirements().keySet()).toLowerCase(Locale.ROOT);
        if (value.contains("3–6") || value.contains("3-6")) return 36;
        if (value.contains("1–3") || value.contains("1-3")) return 12;
        return 0;
    }

    private static VacancyFitAnalysis.ApplyRecommendation recommendation(int must, int grade, int technical,
            List<VacancyFitAnalysis.Blocker> blockers) {
        if (blockers.stream().anyMatch(value -> value.severity() == VacancyFitAnalysis.BlockerSeverity.HIGH)) {
            return VacancyFitAnalysis.ApplyRecommendation.SKIP;
        }
        if (must >= 90 && grade >= 85 && technical >= 85) return VacancyFitAnalysis.ApplyRecommendation.STRONG_APPLY;
        if (must < 50 && grade < 60) return VacancyFitAnalysis.ApplyRecommendation.LOW_PRIORITY;
        if (must < 75 || grade < 70) return VacancyFitAnalysis.ApplyRecommendation.APPLY_WITH_RISK;
        return VacancyFitAnalysis.ApplyRecommendation.APPLY;
    }

    private static ScoreBreakdown.ScoreComponent component(String name, int score, double weight) {
        return new ScoreBreakdown.ScoreComponent(name, score, weight, score * weight, "Vacancy fit component");
    }
}
