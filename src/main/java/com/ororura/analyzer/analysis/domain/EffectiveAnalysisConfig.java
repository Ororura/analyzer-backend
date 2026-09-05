package com.ororura.analyzer.analysis.domain;

import java.time.Instant;
import java.util.UUID;
import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;

/** Persisted before LLM invocation. No references to mutable JPA or Spring runtime objects. */
public record EffectiveAnalysisConfig(int schemaVersion, UserAnalysisProfile profile, UUID marketSnapshotId,
        ScoringPolicy scoringPolicy, MarketAnalysisProfile analysisProfile, VacancyMarketData market,
        AiProviderType provider, String model, String promptVersion, Instant evaluationTime) {}
