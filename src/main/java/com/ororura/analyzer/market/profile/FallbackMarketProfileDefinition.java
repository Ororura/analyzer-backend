package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.List;

import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;

public interface FallbackMarketProfileDefinition {
    ResumeAnalysisProfile profile();
    List<AnalysisCriterion> criteria();
    List<MarketRequirement> requirements();
}
