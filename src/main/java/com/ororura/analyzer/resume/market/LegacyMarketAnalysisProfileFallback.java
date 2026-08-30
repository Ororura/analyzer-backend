package com.ororura.analyzer.resume.market;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.LegacyResumeAnalysisProfileDefinition;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import org.springframework.stereotype.Component;

@Component
public class LegacyMarketAnalysisProfileFallback implements MarketAnalysisProfileFallback {

    private final ResumeAnalysisProfileRegistry profileRegistry;
    private final MarketAnalysisProfileFactory profileFactory;
    private final Instant generatedAt;
    private final ConcurrentMap<ResumeAnalysisProfile, MarketAnalysisProfile> fallbacks = new ConcurrentHashMap<>();

    public LegacyMarketAnalysisProfileFallback(ResumeAnalysisProfileRegistry profileRegistry,
            MarketAnalysisProfileFactory profileFactory, Clock clock) {
        this.profileRegistry = profileRegistry;
        this.profileFactory = profileFactory;
        this.generatedAt = clock.instant();
    }

    @Override
    public MarketAnalysisProfile get(ResumeAnalysisProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Resume analysis profile must be defined");
        }
        return fallbacks.computeIfAbsent(profile, this::create);
    }

    private MarketAnalysisProfile create(ResumeAnalysisProfile profile) {
        LegacyResumeAnalysisProfileDefinition legacy = profileRegistry.get(profile);
        List<AnalysisCriterion> criteria = legacy.criteria().stream()
                .map(value -> new AnalysisCriterion(value.id(), value.label(), value.description(), value.weight(), 0))
                .toList();
        Map<String, Double> baseline = legacy.baselineSkillFrequencies();
        List<MarketRequirement> requirements = legacy.technologies().stream()
                .map(technology -> new MarketRequirement(
                        MarketRequirementId.of(RequirementType.TECHNOLOGY, technology.name()), technology.name(),
                        RequirementType.TECHNOLOGY, baseline.getOrDefault(technology.name(), 0.0)))
                .toList();
        return profileFactory.create(profile, legacy.targetRole(), criteria, requirements, 0, generatedAt);
    }
}
