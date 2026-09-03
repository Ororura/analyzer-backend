package com.ororura.analyzer.market.application.port;

import com.ororura.analyzer.market.domain.*;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;

public interface MarketAnalysisProfileFallback {

    MarketAnalysisProfile get(ResumeAnalysisProfile profile);
}
