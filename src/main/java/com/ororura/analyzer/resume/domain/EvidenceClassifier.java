package com.ororura.analyzer.resume.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.api.SkillEvidenceAnalysis;
import com.ororura.analyzer.resume.api.SkillEvidenceAnalysis.EvidenceStatus;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import org.springframework.stereotype.Component;

@Component
public class EvidenceClassifier {
    private final SkillNormalizer normalizer;

    public EvidenceClassifier(SkillNormalizer normalizer) { this.normalizer = normalizer; }

    public SkillEvidenceAnalysis classify(MarketAnalysisProfile profile, LlmResumeAnalysisResponse llm) {
        Map<String, LlmResumeAnalysisResponse.SkillEvidenceFact> facts = new LinkedHashMap<>();
        llm.skillEvidence().forEach(fact -> facts.put(normalizer.normalizeId(profile.profile(), fact.skill()), fact));
        List<SkillEvidenceAnalysis.SkillEvidence> result = new ArrayList<>();
        for (var requirement : profile.requirements()) {
            String id = normalizer.normalizeId(profile.profile(), requirement.label());
            var fact = facts.get(id);
            List<String> evidence = fact == null ? List.of() : fact.evidence().stream()
                    .map(LlmResumeAnalysisResponse.EvidenceFact::text).filter(value -> value != null && !value.isBlank())
                    .distinct().toList();
            EvidenceStatus status = fact == null
                    ? legacyStatus(llm.skills(), requirement.label()) : status(fact.evidence());
            result.add(new SkillEvidenceAnalysis.SkillEvidence(id, requirement.label(), status,
                    confidence(status, evidence.size()), evidence));
        }
        return new SkillEvidenceAnalysis(result);
    }

    private static EvidenceStatus status(List<LlmResumeAnalysisResponse.EvidenceFact> evidence) {
        long concreteTasks = evidence.stream().filter(item -> item.concrete()
                && (item.context() == LlmResumeAnalysisResponse.EvidenceContext.COMMERCIAL_TASK
                || item.context() == LlmResumeAnalysisResponse.EvidenceContext.PROJECT_TASK)).count();
        boolean commercialOutcome = evidence.stream().anyMatch(item -> item.concrete() && item.hasOutcome()
                && item.context() == LlmResumeAnalysisResponse.EvidenceContext.COMMERCIAL_TASK);
        if (concreteTasks >= 2 || commercialOutcome) return EvidenceStatus.STRONG;
        if (concreteTasks == 1) return EvidenceStatus.MEDIUM;
        if (evidence.stream().anyMatch(item -> item.context()
                == LlmResumeAnalysisResponse.EvidenceContext.STACK_CONTEXT)) return EvidenceStatus.WEAK;
        if (evidence.stream().anyMatch(item -> item.context()
                == LlmResumeAnalysisResponse.EvidenceContext.SKILLS_SECTION)) return EvidenceStatus.MENTION_ONLY;
        return EvidenceStatus.NOT_FOUND;
    }

    private static EvidenceStatus legacyStatus(LlmResumeAnalysisResponse.Skills skills, String value) {
        if (contains(skills.confirmed(), value)) return EvidenceStatus.MEDIUM;
        if (contains(skills.weakEvidence(), value)) return EvidenceStatus.MENTION_ONLY;
        return EvidenceStatus.NOT_FOUND;
    }

    private static boolean contains(List<String> values, String expected) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(expected));
    }

    private static double confidence(EvidenceStatus status, int evidenceCount) {
        double base = switch (status) {
            case STRONG -> .85; case MEDIUM -> .70; case WEAK -> .50; case MENTION_ONLY -> .35; case NOT_FOUND -> .90;
        };
        return Math.min(.99, base + Math.min(3, evidenceCount) * .04);
    }

    public static double weight(EvidenceStatus status) {
        return switch (status) {
            case STRONG -> 1; case MEDIUM -> .8; case WEAK -> .5; case MENTION_ONLY -> .25; case NOT_FOUND -> 0;
        };
    }
}
