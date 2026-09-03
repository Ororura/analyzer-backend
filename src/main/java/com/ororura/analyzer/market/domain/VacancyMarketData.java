package com.ororura.analyzer.market.domain;

import java.util.List;
import java.util.Map;
import com.ororura.analyzer.market.domain.MarketSkillStatistics;

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
        List<MarketSkillStatistics> skillStatistics,
        String marketVersion,
        boolean sufficientSample,
        List<String> warnings) {

    public VacancyMarketData(String source, int sampleSize, Map<String, Double> skillFrequencies,
            Map<String, Integer> experienceRequirements, Map<String, Integer> employmentTypes,
            Map<String, Integer> workFormats, List<String> warnings) {
        this(source, sampleSize, skillFrequencies, experienceRequirements, employmentTypes, workFormats,
                null, List.of(), List.of(), List.of(), null, false, warnings);
    }

    public VacancyMarketData(String source, int sampleSize, Map<String, Double> skillFrequencies,
            Map<String, Integer> experienceRequirements, Map<String, Integer> employmentTypes,
            Map<String, Integer> workFormats, SalaryStatistics salaryStatistics,
            List<String> commonRequirements, List<VacancyContext> vacancies, List<String> warnings) {
        this(source, sampleSize, skillFrequencies, experienceRequirements, employmentTypes, workFormats,
                salaryStatistics, commonRequirements, vacancies, List.of(), null, false, warnings);
    }

    public VacancyMarketData {
        skillFrequencies = skillFrequencies == null ? Map.of() : Map.copyOf(skillFrequencies);
        experienceRequirements = experienceRequirements == null ? Map.of() : Map.copyOf(experienceRequirements);
        employmentTypes = employmentTypes == null ? Map.of() : Map.copyOf(employmentTypes);
        workFormats = workFormats == null ? Map.of() : Map.copyOf(workFormats);
        commonRequirements = commonRequirements == null ? List.of() : List.copyOf(commonRequirements);
        vacancies = vacancies == null ? List.of() : List.copyOf(vacancies);
        skillStatistics = skillStatistics == null ? List.of() : List.copyOf(skillStatistics);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
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
