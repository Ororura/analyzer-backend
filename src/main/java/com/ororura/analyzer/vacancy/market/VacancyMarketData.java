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
        SalaryStatistics salaryStatistics,
        List<String> commonRequirements,
        List<VacancyContext> vacancies,
        List<String> warnings) {

    public VacancyMarketData(String source, int sampleSize, Map<String, Double> skillFrequencies,
            Map<String, Integer> experienceRequirements, Map<String, Integer> employmentTypes,
            Map<String, Integer> workFormats, List<String> warnings) {
        this(source, sampleSize, skillFrequencies, experienceRequirements, employmentTypes, workFormats,
                null, List.of(), List.of(), warnings);
    }

    public record SalaryStatistics(Integer minimum, Integer maximum, Integer averageFrom,
            Integer averageTo, String currency, int samples) {
    }

    public record VacancyContext(String id, String title, List<String> skills, List<String> requirements,
            List<String> responsibilities, String description) {
        public VacancyContext {
            skills = skills == null ? List.of() : List.copyOf(skills);
            requirements = requirements == null ? List.of() : List.copyOf(requirements);
            responsibilities = responsibilities == null ? List.of() : List.copyOf(responsibilities);
        }
    }
}
