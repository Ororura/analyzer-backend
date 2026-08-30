package com.ororura.analyzer.resume.ai;

import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;

import java.util.Optional;

public interface AiProvider {

    AiProviderType type();

    boolean isAvailable();

    Optional<String> model();

    LlmResumeAnalysisResponse analyze(MarketAnalysisProfile profile, String resumeText, VacancyMarketData market);
}
