package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredMarketAnalysisProfileFallback implements MarketAnalysisProfileFallback {
    private final ResumeAnalysisProfileRegistry profiles;
    private final MarketAnalysisProfileFactory factory;
    private final Map<ResumeAnalysisProfile, FallbackMarketProfileDefinition> definitions;
    private final Instant generatedAt;
    private final ConcurrentMap<ResumeAnalysisProfile, MarketAnalysisProfile> cache = new ConcurrentHashMap<>();

    public ConfiguredMarketAnalysisProfileFallback(ResumeAnalysisProfileRegistry profiles,
            MarketAnalysisProfileFactory factory, List<FallbackMarketProfileDefinition> definitions, Clock clock) {
        this.profiles = profiles;
        this.factory = factory;
        EnumMap<ResumeAnalysisProfile, FallbackMarketProfileDefinition> indexed = new EnumMap<>(ResumeAnalysisProfile.class);
        definitions.forEach(value -> {
            if (indexed.putIfAbsent(value.profile(), value) != null) {
                throw new IllegalStateException("Duplicate fallback market profile: " + value.profile());
            }
        });
        this.definitions = Map.copyOf(indexed);
        this.generatedAt = clock.instant();
    }

    @Override public MarketAnalysisProfile get(ResumeAnalysisProfile profile) {
        return cache.computeIfAbsent(profile, this::create);
    }

    private MarketAnalysisProfile create(ResumeAnalysisProfile profile) {
        FallbackMarketProfileDefinition definition = definitions.get(profile);
        if (definition == null) {
            var criterion = new com.ororura.analyzer.analysis.profile.AnalysisCriterion(
                    profile.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-') + "-role-evidence-fallback",
                    profiles.get(profile).targetRole(), "Fallback assessment of evidence relevant to the target role",
                    1, 0);
            return factory.create(profile, profiles.get(profile).targetRole(), List.of(criterion),
                    List.of(), 0, generatedAt);
        }
        return factory.create(profile, profiles.get(profile).targetRole(), definition.criteria(),
                definition.requirements(), 0, generatedAt);
    }
}
