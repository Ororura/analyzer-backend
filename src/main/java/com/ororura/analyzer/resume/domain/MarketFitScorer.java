package com.ororura.analyzer.resume.domain;

import com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.resume.domain.analysis.GradeFitAnalysis;
import com.ororura.analyzer.resume.domain.analysis.MarketFitAnalysis;
import com.ororura.analyzer.resume.domain.analysis.ScoreBreakdown;
import com.ororura.analyzer.resume.domain.analysis.SkillEvidenceAnalysis;
import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.domain.MarketRequirement;
public class MarketFitScorer {
    private final AnalysisScoringPolicy policy;
    public MarketFitScorer(AnalysisScoringPolicy policy) { this.policy = policy; }

    public MarketFitAnalysis score(MarketAnalysisProfile profile, SkillEvidenceAnalysis evidence,
            int commercialScore, int ats, GradeFitAnalysis grade) {
        if (profile.requirements().isEmpty()) {
            return new MarketFitAnalysis(null, null, null, null, grade.targetLevel(),
                    new ScoreBreakdown(null, List.of()));
        }
        Map<String, SkillEvidenceAnalysis.SkillEvidence> byName = evidence.skills().stream()
                .collect(java.util.stream.Collectors.toMap(value -> value.skill().toLowerCase(), value -> value));
        List<MarketRequirement> must = profile.requirements().stream().filter(requirement -> isMust(profile, requirement))
                .toList();
        List<MarketRequirement> nice = profile.requirements().stream().filter(requirement -> !isMust(profile, requirement))
                .filter(requirement -> requirement.frequency() > 0).toList();
        Integer mustCoverage = coverage(must, byName);
        Integer niceCoverage = coverage(nice, byName);
        Integer skillCoverage = coverage(profile.requirements(), byName);
        Integer matched = matchedVacancies(profile, byName);

        List<ComponentInput> inputs = new ArrayList<>();
        add(inputs, "mustHaveCoverage", mustCoverage, policy.mustHaveWeight());
        add(inputs, "skillCoverage", skillCoverage, policy.skillCoverageWeight());
        add(inputs, "experienceFit", commercialScore * 10, policy.experienceWeight());
        add(inputs, "ats", ats, policy.atsWeight());
        if (grade.targetLevel() != null) add(inputs, "gradeFit", grade.score(), policy.gradeWeight());
        double availableWeight = inputs.stream().mapToDouble(ComponentInput::weight).sum();
        int total = clamp(inputs.stream().mapToDouble(value -> value.score() * value.weight()).sum() / availableWeight);
        List<ScoreBreakdown.ScoreComponent> components = inputs.stream().map(value -> {
            double normalizedWeight = value.weight() / availableWeight;
            return new ScoreBreakdown.ScoreComponent(value.name(), value.score(), normalizedWeight,
                    value.score() * normalizedWeight, "Детерминированный компонент market fit");
        }).toList();
        return new MarketFitAnalysis(total, mustCoverage, niceCoverage, matched, grade.targetLevel(),
                new ScoreBreakdown(total, components));
    }

    private boolean isMust(MarketAnalysisProfile profile, MarketRequirement requirement) {
        return profile.skillStatistics().stream().filter(value -> value.displayName().equalsIgnoreCase(requirement.label()))
                .findFirst().map(value -> value.requiredFrequency() >= policy.mustHaveRequiredFrequency())
                .orElse(requirement.frequency() >= policy.coreFrequency());
    }

    private static Integer coverage(List<MarketRequirement> requirements,
            Map<String, SkillEvidenceAnalysis.SkillEvidence> evidence) {
        double available = requirements.stream().mapToDouble(MarketRequirement::frequency).sum();
        if (available <= 0) return null;
        double earned = requirements.stream().mapToDouble(requirement -> requirement.frequency()
                * EvidenceClassifier.weight(status(evidence, requirement.label()))).sum();
        return clamp(earned / available * 100);
    }

    private static SkillEvidenceAnalysis.EvidenceStatus status(
            Map<String, SkillEvidenceAnalysis.SkillEvidence> evidence, String skill) {
        var value = evidence.get(skill.toLowerCase());
        return value == null ? SkillEvidenceAnalysis.EvidenceStatus.NOT_FOUND : value.status();
    }

    private static Integer matchedVacancies(MarketAnalysisProfile profile,
            Map<String, SkillEvidenceAnalysis.SkillEvidence> evidence) {
        if (!profile.sufficientSample() || profile.vacancyRequirements().isEmpty()) return null;
        Map<String, String> labels = profile.requirements().stream()
                .collect(java.util.stream.Collectors.toMap(MarketRequirement::id, MarketRequirement::label));
        long matched = profile.vacancyRequirements().stream().filter(vacancy -> {
            var required = vacancy.requirements().stream()
                    .filter(item -> item.importance() == com.ororura.analyzer.market.requirement.RequirementImportance.REQUIRED)
                    .toList();
            return required.isEmpty() || required.stream().allMatch(item -> EvidenceClassifier.weight(
                    status(evidence, labels.getOrDefault(item.requirementId(), item.requirementId()))) >= .5);
        }).count();
        return clamp((double) matched / profile.vacancyRequirements().size() * 100);
    }

    private static void add(List<ComponentInput> target, String name, Integer score, double weight) {
        if (score != null && weight > 0) target.add(new ComponentInput(name, score, weight));
    }
    private static int clamp(double value) { return (int) Math.clamp(Math.round(value), 0, 100); }
    private record ComponentInput(String name, int score, double weight) {}
}
