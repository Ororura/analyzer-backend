package com.ororura.analyzer.resume.market;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

public interface MarketAnalysisProfileProvider {

    ResolvedMarketAnalysisProfile resolve(ResumeAnalysisProfile profile);

    default MarketAnalysisProfile getCurrent(ResumeAnalysisProfile profile) {
        return resolve(profile).profile();
    }
}
