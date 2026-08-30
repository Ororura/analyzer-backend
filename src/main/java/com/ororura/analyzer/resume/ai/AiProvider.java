package com.ororura.analyzer.resume.ai;

import com.ororura.analyzer.vacancy.market.VacancyMarketData;

import java.util.Optional;

public interface AiProvider {

    AiProviderType type();

    boolean isAvailable();

    Optional<String> model();

    LlmResumeAnalysisResponse analyze(ResumeAnalysisProfile profile, String resumeText, VacancyMarketData market);

    default LlmResumeAnalysisResponse analyze(String resumeText, VacancyMarketData market) {
        return analyze(ResumeAnalysisProfile.defaultProfile(), resumeText, market);
    }
}
