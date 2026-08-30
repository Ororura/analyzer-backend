package com.ororura.analyzer.resume.ai.profile;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.AnalysisTechnology;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileDefinition;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class JavaBackendAnalysisProfile implements ResumeAnalysisProfileDefinition {

    private final String targetRole;

    @Autowired
    public JavaBackendAnalysisProfile(ResumeAnalysisProperties properties) {
        this.targetRole = properties.targetRole();
    }

    public JavaBackendAnalysisProfile() {
        this.targetRole = "Java Backend Developer";
    }

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.JAVA_BACKEND; }
    @Override public String targetRole() { return targetRole; }

    @Override
    public List<AnalysisCriterion> criteria() {
        return List.of(
                criterion("javaDepth", "Java", "глубина подтверждённой работы с Java в backend-задачах, включая API, collections, streams, exceptions, concurrency или JVM-related задачи, только если они явно описаны", .20),
                criterion("springDepth", "Spring", "подтверждённое применение Spring/Spring Boot: Web, Data, Security, transactions, configuration и integrations. Одного упоминания Spring недостаточно для высокого score", .15),
                criterion("backendDepth", "Backend", "разработка backend-сервисов, API, бизнес-логики, интеграций, архитектуры, производительности и надёжности; общий title Backend Developer сам по себе недостаточен", .15),
                criterion("sqlPostgresqlDepth", "SQL/PostgreSQL", "практическая работа с SQL/PostgreSQL: запросы, joins, схема, индексы, транзакции, миграции и оптимизация; список технологий без задач является слабым evidence", .10),
                criterion("hibernateJpaDepth", "Hibernate/JPA", "mappings, relationships, fetching, queries, transactions и performance в Hibernate/JPA; простое упоминание ORM недостаточно", .10),
                criterion("infrastructureDepth", "Infrastructure", "подтверждённые deployment/operations-задачи с Docker, Kubernetes, Linux, CI/CD или observability; перечень инструментов без выполненных задач является слабым evidence", .10),
                criterion("messagingCacheDepth", "Messaging/cache", "практическое применение Kafka, RabbitMQ, Redis, messaging patterns или cache; общее упоминание технологии не подтверждает глубину", .10),
                criterion("testingDepth", "Testing", "unit/integration/component tests, JUnit, Mockito, Testcontainers, test strategy; учитывай конкретные тестовые задачи, а не только названия библиотек", .10));
    }

    @Override
    public List<AnalysisTechnology> technologies() {
        return technologyDefinitions();
    }

    public static List<AnalysisTechnology> technologyDefinitions() {
        return List.of(
                tech("Java", AnalysisTechnology.Tier.CORE, "\\bjava(?:\\s+(?:8|11|17|21|25))?\\b"),
                tech("Spring Boot", AnalysisTechnology.Tier.CORE, "\\bspring\\s*boot\\b|\\bspring framework\\b|\\bspring\\b"),
                tech("REST API", AnalysisTechnology.Tier.CORE, "\\brest(?:ful)?(?:\\s+api|\\s+endpoints?|\\s+services?)?\\b|\\bweb api\\b"),
                tech("PostgreSQL", AnalysisTechnology.Tier.CORE, "\\bpostgres(?:ql)?\\b"),
                tech("SQL", AnalysisTechnology.Tier.CORE, "\\b(?:sql|mysql|oracle|mariadb)\\b"),
                tech("Git", AnalysisTechnology.Tier.CORE, "\\bgit\\b"),
                tech("Backend development", AnalysisTechnology.Tier.CORE, "backend(?: development)?"),
                tech("Hibernate/JPA", AnalysisTechnology.Tier.COMMON, "\\bhibernate\\b|\\bjpa\\b|jakarta persistence"),
                tech("Docker", AnalysisTechnology.Tier.COMMON, "\\bdocker\\b"),
                tech("Testing", AnalysisTechnology.Tier.COMMON, "\\bjunit\\b|\\bmockito\\b|\\btestcontainers?\\b|unit test|integration test"),
                tech("Maven/Gradle", AnalysisTechnology.Tier.COMMON, "\\bmaven\\b|\\bgradle\\b"),
                tech("Spring Data", AnalysisTechnology.Tier.COMMON, "spring\\s+data"),
                tech("Spring Security", AnalysisTechnology.Tier.COMMON, "spring\\s+security"),
                tech("Kafka", AnalysisTechnology.Tier.BONUS, "\\bkafka\\b|apache kafka"),
                tech("RabbitMQ", AnalysisTechnology.Tier.BONUS, "rabbit\\s*mq"),
                tech("Redis", AnalysisTechnology.Tier.BONUS, "\\bredis\\b"),
                tech("Kubernetes", AnalysisTechnology.Tier.BONUS, "\\bk8s\\b|\\bkubernetes\\b"),
                tech("Microservices", AnalysisTechnology.Tier.BONUS, "microservices?"),
                tech("Prometheus", AnalysisTechnology.Tier.BONUS, "prometheus"),
                tech("Grafana", AnalysisTechnology.Tier.BONUS, "grafana"),
                tech("Linux", AnalysisTechnology.Tier.BONUS, "\\blinux\\b|\\bubuntu\\b|\\bcentos\\b|\\bdebian\\b"),
                tech("CI/CD", AnalysisTechnology.Tier.BONUS, "ci\\s*/?\\s*cd|continuous integration|gitlab ci(?:/cd)?|github actions|jenkins"));
    }

    @Override
    public Map<String, Double> baselineSkillFrequencies() {
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

    private static AnalysisCriterion criterion(String id, String label, String description, double weight) {
        return new AnalysisCriterion(id, label, description, weight);
    }

    private static AnalysisTechnology tech(String name, AnalysisTechnology.Tier tier, String... patterns) {
        return new AnalysisTechnology(name, tier, List.of(patterns));
    }
}
