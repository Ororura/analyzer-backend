package com.ororura.analyzer.resume.ai;

import com.ororura.analyzer.vacancy.market.VacancyMarketData;

public interface AiProvider {

    LlmResumeAnalysisResponse analyze(String resumeText, VacancyMarketData market);
}
