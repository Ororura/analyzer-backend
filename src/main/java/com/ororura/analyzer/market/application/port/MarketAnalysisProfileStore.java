package com.ororura.analyzer.market.application.port;

import com.ororura.analyzer.market.domain.*;

import java.util.Optional;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;

public interface MarketAnalysisProfileStore {

    Optional<MarketAnalysisProfile> current(ResumeAnalysisProfile profile);

    Optional<MarketAnalysisProfile> previous(ResumeAnalysisProfile profile);

    /**
     * Atomically makes the supplied complete profile current and retains the former current profile as previous.
     * Implementations must leave the current profile unchanged when publishing fails.
     */
    void publish(MarketAnalysisProfile profile);
}
