package com.ororura.analyzer.resume.api;

import java.util.List;

public record SkillEvidenceAnalysis(List<SkillEvidence> skills) {
    public SkillEvidenceAnalysis { skills = List.copyOf(skills); }
    public record SkillEvidence(String skillId, String skill, EvidenceStatus status, double confidence,
            List<String> evidence) {
        public SkillEvidence { evidence = List.copyOf(evidence); }
    }
    public enum EvidenceStatus { STRONG, MEDIUM, WEAK, MENTION_ONLY, NOT_FOUND }
}
