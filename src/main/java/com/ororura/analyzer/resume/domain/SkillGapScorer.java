package com.ororura.analyzer.resume.domain;

import com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.ororura.analyzer.resume.domain.analysis.SkillEvidenceAnalysis;
import com.ororura.analyzer.resume.domain.analysis.SkillGapAnalysis;
import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
public class SkillGapScorer {
    private final AnalysisScoringPolicy policy;
    public SkillGapScorer(AnalysisScoringPolicy policy) { this.policy = policy; }

    public SkillGapAnalysis score(MarketAnalysisProfile profile, SkillEvidenceAnalysis evidence) {
        Map<String, SkillEvidenceAnalysis.SkillEvidence> bySkill = evidence.skills().stream()
                .collect(java.util.stream.Collectors.toMap(value -> value.skill().toLowerCase(), value -> value));
        double totalFrequency = profile.requirements().stream().mapToDouble(value -> value.frequency()).sum();
        if (totalFrequency <= 0) return new SkillGapAnalysis(List.of());
        var gaps = profile.requirements().stream().map(requirement -> {
            var candidate = bySkill.get(requirement.label().toLowerCase());
            var status = candidate == null ? SkillEvidenceAnalysis.EvidenceStatus.NOT_FOUND : candidate.status();
            double gap = 1 - EvidenceClassifier.weight(status);
            double importance = profile.skillStatistics().stream()
                    .filter(value -> value.displayName().equalsIgnoreCase(requirement.label())).findFirst()
                    .map(value -> value.requiredFrequency() > 0 ? 1.0
                            : value.preferredFrequency() > 0 ? .75 : .4).orElse(.75);
            double impact = requirement.frequency() * gap * importance;
            var priority = impact >= policy.highGapThreshold() ? SkillGapAnalysis.GapPriority.HIGH
                    : impact >= policy.mediumGapThreshold() ? SkillGapAnalysis.GapPriority.MEDIUM
                    : SkillGapAnalysis.GapPriority.LOW;
            double gain = Math.round(requirement.frequency() * gap / totalFrequency * 1000) / 1000.0;
            return new SkillGapAnalysis.SkillGap(candidate == null ? normalized(requirement.label()) : candidate.skillId(),
                    requirement.label(), requirement.frequency(), status, priority, gain);
        }).filter(value -> value.candidateStatus() != SkillEvidenceAnalysis.EvidenceStatus.STRONG)
                .sorted(Comparator.comparing(SkillGapAnalysis.SkillGap::priority)
                        .thenComparing(SkillGapAnalysis.SkillGap::marketFrequency, Comparator.reverseOrder()))
                .toList();
        return new SkillGapAnalysis(gaps);
    }

    private static String normalized(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
