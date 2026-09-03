package com.ororura.analyzer.resume.domain;

import java.util.Comparator;
import java.util.Locale;
import com.ororura.analyzer.resume.domain.analysis.SkillGapAnalysis;
import com.ororura.analyzer.resume.domain.analysis.SkillRoiAnalysis;
import com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy;
public class SkillRoiCalculator {
    private final AnalysisScoringPolicy policy;
    public SkillRoiCalculator(AnalysisScoringPolicy policy) { this.policy = policy; }
    public SkillRoiAnalysis calculate(SkillGapAnalysis gaps) {
        return new SkillRoiAnalysis(gaps.gaps().stream().map(gap -> {
            var effort = effort(gap.skill());
            double gapWeight = switch (gap.priority()) {
                case HIGH -> policy.highGapWeight();
                case MEDIUM -> policy.mediumGapWeight();
                case LOW -> policy.lowGapWeight();
            };
            double effortWeight = switch (effort) {
                case LOW -> policy.lowEffortWeight();
                case MEDIUM -> policy.mediumEffortWeight();
                case HIGH -> policy.highEffortWeight();
            };
            double roi = Math.clamp(10 * gap.marketFrequency() * gapWeight
                    * gap.estimatedCoverageGain() / effortWeight, 0, 10);
            roi = Math.round(roi * 10) / 10.0;
            return new SkillRoiAnalysis.SkillRoi(gap.skillId(), gap.skill(), roi, gap.marketFrequency(),
                    gap.priority(), effort, gap.estimatedCoverageGain());
        }).sorted(Comparator.comparingDouble(SkillRoiAnalysis.SkillRoi::roiScore).reversed()).toList());
    }

    private static SkillRoiAnalysis.Effort effort(String skill) {
        String value = skill.toLowerCase(Locale.ROOT);
        if (value.contains("testcontainers")) return SkillRoiAnalysis.Effort.LOW;
        if (value.contains("kafka")) return SkillRoiAnalysis.Effort.MEDIUM;
        if (value.contains("kubernetes")) return SkillRoiAnalysis.Effort.HIGH;
        return SkillRoiAnalysis.Effort.HIGH;
    }
}
