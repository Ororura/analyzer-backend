package com.ororura.analyzer.resume.domain.analysis;

import java.util.List;
import com.ororura.analyzer.resume.domain.analysis.SkillGapAnalysis.GapPriority;

public record SkillRoiAnalysis(List<SkillRoi> skills) {
    public SkillRoiAnalysis { skills = List.copyOf(skills); }
    public record SkillRoi(String skillId, String skill, double roiScore, double marketDemand,
            GapPriority currentGap, Effort learningEffort, double estimatedCoverageGain) {
    }
    public enum Effort { LOW, MEDIUM, HIGH }
}
