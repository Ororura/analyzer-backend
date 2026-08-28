package com.ororura.analyzer.vacancy.market;

import java.util.List;
import java.util.Map;

public record VacancyMarketData(
        String source,
        int sampleSize,
        Map<String, Double> skillFrequencies,
        Map<String, Integer> experienceRequirements,
        Map<String, Integer> employmentTypes,
        Map<String, Integer> workFormats,
        List<String> warnings) {
}
