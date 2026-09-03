package com.ororura.analyzer.market.application;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.Optional;
import java.util.function.Supplier;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultMarketAnalysisProfileProvider implements MarketAnalysisProfileProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultMarketAnalysisProfileProvider.class);
    private final MarketAnalysisProfileStore store;
    private final MarketAnalysisProfileFallback fallback;

    public DefaultMarketAnalysisProfileProvider(MarketAnalysisProfileStore store,
            MarketAnalysisProfileFallback fallback) {
        this.store = store;
        this.fallback = fallback;
    }

    @Override
    public ResolvedMarketAnalysisProfile resolve(ResumeAnalysisProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Resume analysis profile must be defined");
        }
        Optional<MarketAnalysisProfile> current = read(profile, "current", () -> store.current(profile));
        if (current.isPresent()) {
            log.info("Market profile resolved profile={} source=LIVE version={}", profile, current.get().version());
            return new ResolvedMarketAnalysisProfile(current.get(), MarketProfileSource.LIVE);
        }
        Optional<MarketAnalysisProfile> previous = read(profile, "previous", () -> store.previous(profile));
        if (previous.isPresent()) {
            log.info("Market profile resolved profile={} source=CACHED version={}", profile, previous.get().version());
            return new ResolvedMarketAnalysisProfile(previous.get(), MarketProfileSource.CACHED);
        }
        MarketAnalysisProfile fallbackProfile = fallback.get(profile);
        log.info("Market profile resolved profile={} source=FALLBACK version={}", profile, fallbackProfile.version());
        return new ResolvedMarketAnalysisProfile(fallbackProfile, MarketProfileSource.FALLBACK);
    }

    private static Optional<MarketAnalysisProfile> read(ResumeAnalysisProfile profile, String tier,
            Supplier<Optional<MarketAnalysisProfile>> reader) {
        try {
            return reader.get();
        } catch (RuntimeException exception) {
            log.warn("Market analysis profile store unavailable profile={} tier={}; trying fallback",
                    profile, tier, exception);
            return Optional.empty();
        }
    }
}
