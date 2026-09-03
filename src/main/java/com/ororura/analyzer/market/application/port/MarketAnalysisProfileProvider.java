package com.ororura.analyzer.market.application.port;

import com.ororura.analyzer.market.domain.*;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;

public interface MarketAnalysisProfileProvider {

    ResolvedMarketAnalysisProfile resolve(ResumeAnalysisProfile profile);

    default MarketAnalysisProfile getCurrent(ResumeAnalysisProfile profile) {
        return resolve(profile).profile();
    }
}
