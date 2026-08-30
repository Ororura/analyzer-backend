package com.ororura.analyzer.resume.market;

import java.util.List;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

public interface FallbackMarketProfileDefinition {
    ResumeAnalysisProfile profile();
    List<AnalysisCriterion> criteria();
    List<MarketRequirement> requirements();
}
