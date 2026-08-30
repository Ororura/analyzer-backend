package com.ororura.analyzer.resume.ai;

import java.time.Instant;
import java.util.List;
import com.ororura.analyzer.resume.market.FallbackMarketProfileDefinition;
import com.ororura.analyzer.resume.market.JavaBackendFallbackMarketProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileFactory;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.ReactFrontendFallbackMarketProfile;

final class ResumeAnalysisMarketProfileFixture {

    private ResumeAnalysisMarketProfileFixture() {
    }

    static MarketAnalysisProfile from(ResumeAnalysisProfileDefinition definition) {
        var catalog = new DefaultAnalysisTechnologyCatalog();
        FallbackMarketProfileDefinition fallback = switch (definition.profile()) {
            case JAVA_BACKEND -> new JavaBackendFallbackMarketProfile(catalog);
            case REACT_FRONTEND -> new ReactFrontendFallbackMarketProfile(catalog);
        };
        return new MarketAnalysisProfileFactory().create(definition.profile(), definition.targetRole(),
                fallback.criteria(), fallback.requirements(), 0, Instant.parse("2026-08-28T07:00:00Z"));
    }
}
