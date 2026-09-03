package com.ororura.analyzer.market.infrastructure.store;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import org.springframework.stereotype.Component;

@Component
public class InMemoryMarketAnalysisProfileStore implements MarketAnalysisProfileStore {

    private final ConcurrentMap<ResumeAnalysisProfile, Profiles> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<MarketAnalysisProfile> current(ResumeAnalysisProfile profile) {
        Profiles resolved = profiles.get(profile);
        return resolved == null ? Optional.empty() : Optional.of(resolved.current());
    }

    @Override
    public Optional<MarketAnalysisProfile> previous(ResumeAnalysisProfile profile) {
        Profiles resolved = profiles.get(profile);
        return resolved == null ? Optional.empty() : Optional.ofNullable(resolved.previous());
    }

    @Override
    public void publish(MarketAnalysisProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Market analysis profile must be defined");
        }
        profiles.compute(profile.profile(), (key, existing) -> {
            if (existing == null) {
                return new Profiles(profile, null);
            }
            if (existing.current().version().equals(profile.version())) {
                return existing;
            }
            return new Profiles(profile, existing.current());
        });
    }

    private record Profiles(MarketAnalysisProfile current, MarketAnalysisProfile previous) {
    }
}
