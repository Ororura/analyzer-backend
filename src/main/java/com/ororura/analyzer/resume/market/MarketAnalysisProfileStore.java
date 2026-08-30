package com.ororura.analyzer.resume.market;

import java.util.Optional;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

public interface MarketAnalysisProfileStore {

    Optional<MarketAnalysisProfile> current(ResumeAnalysisProfile profile);

    Optional<MarketAnalysisProfile> previous(ResumeAnalysisProfile profile);

    void publish(MarketAnalysisProfile profile);
}
