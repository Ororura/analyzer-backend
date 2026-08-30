package com.ororura.analyzer.vacancy.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.ororura.analyzer.resume.domain.TechnologyTaxonomy;
import org.springframework.stereotype.Component;

@Component
public class VacancyMarketAggregator {

    private static final TechnologyTaxonomy TAXONOMY = new TechnologyTaxonomy();

    private static final Set<String> TECHNOLOGIES = Set.copyOf(TAXONOMY.tiers().keySet());

    private static final Map<String, Double> BASELINE_FREQUENCIES = baselineFrequencies();

    public VacancyMarketData aggregate(List<MarketVacancy> vacancies, List<String> warnings) {
        return aggregate(vacancies, warnings, "live");
    }

    public VacancyMarketData aggregate(List<MarketVacancy> vacancies, List<String> warnings, String source) {
        if (vacancies.isEmpty()) {
            return baseline(warnings.isEmpty() ? "HH.ru не вернул актуальные вакансии" : warnings.getFirst());
        }

        Map<String, Integer> skillCounts = new LinkedHashMap<>();
        Map<String, Integer> experienceRequirements = new LinkedHashMap<>();
        Map<String, Integer> employmentTypes = new LinkedHashMap<>();
        Map<String, Integer> workFormats = new LinkedHashMap<>();
        Map<String, Integer> requirementCounts = new LinkedHashMap<>();

        for (MarketVacancy vacancy : vacancies) {
            Set<String> vacancySkills = new LinkedHashSet<>();
            for (String skill : vacancy.skills()) {
                String canonical = canonicalTechnology(skill);
                if (TECHNOLOGIES.contains(canonical)) {
                    vacancySkills.add(canonical);
                }
            }
            vacancySkills.forEach(skill -> increment(skillCounts, skill));
            increment(experienceRequirements, vacancy.experience());
            increment(employmentTypes, vacancy.employment());
            increment(workFormats, displayWorkFormat(firstNonBlank(vacancy.workFormat(), vacancy.schedule())));
            vacancy.requirements().stream().map(String::trim).filter(value -> !value.isBlank())
                    .map(value -> truncate(value, 500)).distinct()
                    .forEach(value -> increment(requirementCounts, value));
        }

        Map<String, Double> frequencies = "live".equals(source)
                ? new LinkedHashMap<>(BASELINE_FREQUENCIES) : new LinkedHashMap<>();
        skillCounts.forEach((skill, count) -> frequencies.put(skill, roundShare(count, vacancies.size())));
        List<String> commonRequirements = requirementCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(20).map(Map.Entry::getKey).toList();
        List<VacancyMarketData.VacancyContext> contexts = vacancies.size() == 1
                ? List.of(toContext(vacancies.getFirst())) : List.of();
        return new VacancyMarketData(source, vacancies.size(), frequencies, experienceRequirements,
                employmentTypes, workFormats, salaryStatistics(vacancies), commonRequirements, contexts,
                List.copyOf(warnings));
    }

    public VacancyMarketData baseline(String warning) {
        return new VacancyMarketData("baseline", 0, new LinkedHashMap<>(BASELINE_FREQUENCIES),
                Map.of(), Map.of(), Map.of(), null, List.of(), List.of(),
                warning == null || warning.isBlank() ? List.of() : List.of(warning));
    }

    private static VacancyMarketData.VacancyContext toContext(MarketVacancy vacancy) {
        return new VacancyMarketData.VacancyContext(vacancy.id(), vacancy.title(), vacancy.skills(),
                vacancy.requirements(), vacancy.responsibilities(), truncate(vacancy.description(), 12_000));
    }

    private static VacancyMarketData.SalaryStatistics salaryStatistics(List<MarketVacancy> vacancies) {
        var salaries = vacancies.stream().map(MarketVacancy::salary).filter(java.util.Objects::nonNull).toList();
        if (salaries.isEmpty()) return null;
        List<Integer> from = salaries.stream().map(com.ororura.analyzer.vacancy.model.Salary::from)
                .filter(java.util.Objects::nonNull).toList();
        List<Integer> to = salaries.stream().map(com.ororura.analyzer.vacancy.model.Salary::to)
                .filter(java.util.Objects::nonNull).toList();
        List<Integer> all = new ArrayList<>(from); all.addAll(to);
        String currency = salaries.stream().map(com.ororura.analyzer.vacancy.model.Salary::currency)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(value -> value, Collectors.counting())).entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        return new VacancyMarketData.SalaryStatistics(all.stream().min(Integer::compareTo).orElse(null),
                all.stream().max(Integer::compareTo).orElse(null), average(from), average(to), currency, salaries.size());
    }

    private static Integer average(List<Integer> values) {
        return values.isEmpty() ? null : (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    String canonicalTechnology(String value) {
        return TAXONOMY.canonical(value);
    }

    private static void increment(Map<String, Integer> target, String value) {
        if (value != null && !value.isBlank()) {
            target.merge(value, 1, Integer::sum);
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String displayWorkFormat(String value) {
        if (value == null) return null;
        return switch (value) {
            case "REMOTE" -> "Удалённо";
            case "HYBRID" -> "Гибрид";
            case "OFFICE" -> "На месте";
            default -> value;
        };
    }

    private static double roundShare(int count, int total) {
        return BigDecimal.valueOf((double) count / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static Map<String, Double> baselineFrequencies() {
        Map<String, Double> frequencies = new LinkedHashMap<>();
        frequencies.put("Java", 0.93);
        frequencies.put("Spring Boot", 0.89);
        frequencies.put("REST API", 0.82);
        frequencies.put("SQL", 0.81);
        frequencies.put("PostgreSQL", 0.68);
        frequencies.put("Git", 0.65);
        frequencies.put("Backend development", 0.90);
        frequencies.put("Hibernate/JPA", 0.61);
        frequencies.put("Docker", 0.54);
        frequencies.put("Testing", 0.52);
        frequencies.put("Maven/Gradle", 0.49);
        frequencies.put("Spring Data", 0.45);
        frequencies.put("Spring Security", 0.39);
        frequencies.put("Kafka", 0.41);
        frequencies.put("RabbitMQ", 0.18);
        frequencies.put("Redis", 0.27);
        frequencies.put("Kubernetes", 0.19);
        frequencies.put("Microservices", 0.35);
        frequencies.put("Prometheus", 0.14);
        frequencies.put("Grafana", 0.13);
        frequencies.put("Linux", 0.29);
        frequencies.put("CI/CD", 0.31);
        return Map.copyOf(frequencies);
    }
}
