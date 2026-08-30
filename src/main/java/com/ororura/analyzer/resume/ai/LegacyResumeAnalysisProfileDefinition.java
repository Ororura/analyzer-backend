package com.ororura.analyzer.resume.ai;

import java.util.List;
import java.util.Map;

/**
 * Transitional contract for the hardcoded resume analysis profile.
 *
 * <p>New market-driven integrations should depend on {@link ResumeAnalysisProfileDefinition}
 * and {@code MarketAnalysisProfileProvider} instead.</p>
 */
public interface LegacyResumeAnalysisProfileDefinition extends ResumeAnalysisProfileDefinition {

    List<AnalysisCriterion> criteria();

    List<AnalysisTechnology> technologies();

    default Map<String, Double> baselineSkillFrequencies() {
        return Map.of();
    }
}
