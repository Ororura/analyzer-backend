package com.ororura.analyzer.resume.application.port;

import com.ororura.analyzer.analysis.semantic.LlmResumeAnalysisResponse;

import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.domain.VacancyMarketData;

import java.util.Optional;

public interface AiProvider {

    AiProviderType type();

    boolean isAvailable();

    Optional<String> model();

    LlmResumeAnalysisResponse analyze(MarketAnalysisProfile profile, String resumeText, VacancyMarketData market);
}
