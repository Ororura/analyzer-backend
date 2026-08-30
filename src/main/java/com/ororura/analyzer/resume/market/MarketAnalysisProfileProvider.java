package com.ororura.analyzer.resume.market;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

public interface MarketAnalysisProfileProvider {

    MarketAnalysisProfile getCurrent(ResumeAnalysisProfile profile);
}
