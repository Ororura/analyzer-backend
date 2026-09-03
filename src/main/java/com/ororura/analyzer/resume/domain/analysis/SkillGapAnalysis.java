package com.ororura.analyzer.resume.domain.analysis;

import java.util.List;
import com.ororura.analyzer.resume.domain.analysis.SkillEvidenceAnalysis.EvidenceStatus;

public record SkillGapAnalysis(List<SkillGap> gaps) {
    public SkillGapAnalysis { gaps = List.copyOf(gaps); }
    public record SkillGap(String skillId, String skill, double marketFrequency, EvidenceStatus candidateStatus,
            GapPriority priority, double estimatedCoverageGain) {
    }
    public enum GapPriority { HIGH, MEDIUM, LOW }
}
