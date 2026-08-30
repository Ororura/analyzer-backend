package com.ororura.analyzer.resume.market;

import java.util.Optional;
import java.util.function.Supplier;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
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
    public MarketAnalysisProfile getCurrent(ResumeAnalysisProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Resume analysis profile must be defined");
        }
        Optional<MarketAnalysisProfile> current = read(profile, "current", () -> store.current(profile));
        if (current.isPresent()) {
            return current.get();
        }
        Optional<MarketAnalysisProfile> previous = read(profile, "previous", () -> store.previous(profile));
        return previous.orElseGet(() -> fallback.get(profile));
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
