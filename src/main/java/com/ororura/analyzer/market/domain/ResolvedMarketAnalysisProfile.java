package com.ororura.analyzer.market.domain;

public record ResolvedMarketAnalysisProfile(MarketAnalysisProfile profile, MarketProfileSource source) {
    public ResolvedMarketAnalysisProfile {
        if (profile == null || source == null) throw new IllegalArgumentException("Resolved market profile is required");
    }
}
