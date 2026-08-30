package com.ororura.analyzer.resume.market;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

public interface MarketAnalysisProfileFallback {

    MarketAnalysisProfile get(ResumeAnalysisProfile profile);
}
