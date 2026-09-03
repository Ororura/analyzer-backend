package com.ororura.analyzer.resume.ai;

import com.ororura.analyzer.analysis.profile.DefaultAnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileDefinition;

import java.time.Instant;
import java.util.List;
import com.ororura.analyzer.market.profile.FallbackMarketProfileDefinition;
import com.ororura.analyzer.market.profile.JavaBackendFallbackMarketProfile;
import com.ororura.analyzer.market.domain.MarketAnalysisProfileFactory;
import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.profile.ReactFrontendFallbackMarketProfile;

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
