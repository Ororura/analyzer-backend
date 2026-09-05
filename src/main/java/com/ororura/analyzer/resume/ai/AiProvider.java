package com.ororura.analyzer.resume.ai;

import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;

import java.util.Optional;

public interface AiProvider {

    AiProviderType type();

    boolean isAvailable();

    Optional<String> model();

    default LlmResumeAnalysisResponse analyze(com.ororura.analyzer.analysis.domain.EffectiveAnalysisConfig config,
            String resumeText) {
        return analyze(config.analysisProfile(), resumeText, config.market());
    }

    LlmResumeAnalysisResponse analyze(MarketAnalysisProfile profile, String resumeText, VacancyMarketData market);
}
