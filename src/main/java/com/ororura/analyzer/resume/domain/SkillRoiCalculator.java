package com.ororura.analyzer.resume.domain;

import java.util.Comparator;
import java.util.Locale;
import com.ororura.analyzer.resume.api.SkillGapAnalysis;
import com.ororura.analyzer.resume.api.SkillRoiAnalysis;
import org.springframework.stereotype.Component;
import com.ororura.analyzer.resume.config.ResumeScoringProperties;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class SkillRoiCalculator {
    private final ResumeScoringProperties properties;
    public SkillRoiCalculator() { this(new ResumeScoringProperties()); }
    @Autowired public SkillRoiCalculator(ResumeScoringProperties properties) { this.properties = properties; }
    public SkillRoiAnalysis calculate(SkillGapAnalysis gaps) {
        return new SkillRoiAnalysis(gaps.gaps().stream().map(gap -> {
            var effort = effort(gap.skill());
            double gapWeight = properties.gapWeight(gap.priority());
            double effortWeight = properties.effortWeight(effort);
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
