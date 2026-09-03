package com.ororura.analyzer.market.profile;

import java.util.List;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.market.domain.RequirementType;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics.MappedRequirement;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics.RequirementStatistics;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics.VacancyRequirements;
import com.ororura.analyzer.market.requirement.RequirementImportance;

final class MarketCriterionFixture {

    private MarketCriterionFixture() {
    }

    static MarketRequirementStatistics statistics() {
        return statistics(false);
    }

    static MarketRequirementStatistics statistics(boolean expandedSpringCoverage) {
        List<VacancyRequirements> mappings = List.of(
                vacancy("v1", mapped("technology:java", RequirementImportance.REQUIRED),
                        mapped("technology:spring-boot", RequirementImportance.REQUIRED)),
                vacancy("v2", mapped("technology:java", RequirementImportance.REQUIRED),
                        mapped("technology:spring-boot", RequirementImportance.PREFERRED)),
                vacancy("v3", mapped("technology:java", RequirementImportance.REQUIRED),
                        mapped("technology:docker", RequirementImportance.REQUIRED),
                        expandedSpringCoverage
                                ? mapped("technology:spring-boot", RequirementImportance.PREFERRED) : null),
                vacancy("v4", mapped("technology:sql", RequirementImportance.REQUIRED),
                        mapped("technology:kafka", RequirementImportance.OPTIONAL),
                        mapped("practice:testing", RequirementImportance.PREFERRED)));
        int springTotal = expandedSpringCoverage ? 3 : 2;
        return new MarketRequirementStatistics(ResumeAnalysisProfile.JAVA_BACKEND,
                4, 4, 4, 0, true, mappings, List.of(
                requirement("technology:java", "Java", 3, 3, 0, 0),
                requirement("technology:spring-boot", "Spring Boot", springTotal, 1, springTotal - 1, 0),
                requirement("technology:sql", "SQL", 1, 1, 0, 0),
                requirement("technology:docker", "Docker", 1, 1, 0, 0),
                requirement("technology:kafka", "Kafka", 1, 0, 0, 1),
                requirement("practice:testing", "Testing", 1, 0, 1, 0),
                requirement("tool:sonarqube", "SonarQube", 0, 0, 0, 0)));
    }

    static List<GeneratedCriterionGroup> groups() {
        return List.of(
                group("Java", "Практическая разработка на Java", "technology:java"),
                group("Spring Backend", "Backend-сервисы на Spring", "technology:spring-boot"),
                group("Databases", "Работа с SQL и базами данных", "technology:sql"),
                group("Infrastructure", "Контейнеризация и эксплуатация", "technology:docker"),
                group("Messaging", "Асинхронное взаимодействие", "technology:kafka"),
                group("Testing", "Автоматизированное тестирование", "practice:testing"));
    }

    static GeneratedCriterionGroup group(String label, String description, String... ids) {
        return new GeneratedCriterionGroup(label, description, List.of(ids));
    }

    static String validResponse() {
        return """
                {"criteria":[
                  {"label":"Java","description":"Практическая разработка на Java","requirementIds":["technology:java"]},
                  {"label":"Spring Backend","description":"Backend-сервисы на Spring","requirementIds":["technology:spring-boot"]},
                  {"label":"Databases","description":"Работа с SQL и базами данных","requirementIds":["technology:sql"]},
                  {"label":"Infrastructure","description":"Контейнеризация и эксплуатация","requirementIds":["technology:docker"]},
                  {"label":"Messaging","description":"Асинхронное взаимодействие","requirementIds":["technology:kafka"]},
                  {"label":"Testing","description":"Автоматизированное тестирование","requirementIds":["practice:testing"]}
                ]}
                """;
    }

    private static VacancyRequirements vacancy(String id, MappedRequirement... values) {
        return new VacancyRequirements(id, java.util.Arrays.stream(values)
                .filter(java.util.Objects::nonNull).toList());
    }

    private static MappedRequirement mapped(String id, RequirementImportance importance) {
        return new MappedRequirement(id, importance);
    }

    private static RequirementStatistics requirement(String id, String label, int total,
            int required, int preferred, int optional) {
        int sample = 4;
        return new RequirementStatistics(id, label,
                id.startsWith("practice:") ? RequirementType.PRACTICE : RequirementType.TECHNOLOGY,
                total, required, preferred, optional, (double) total / sample,
                (double) required / sample, (double) preferred / sample, (double) optional / sample);
    }
}
