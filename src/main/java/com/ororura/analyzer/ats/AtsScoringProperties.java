package com.ororura.analyzer.ats;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static com.ororura.analyzer.ats.AtsModels.TechnologyStatus;
import static com.ororura.analyzer.ats.AtsModels.TechnologyTier;

public final class AtsScoringProperties {

    public static final Map<TechnologyStatus, Double> TECHNOLOGY_STATUS_WEIGHT = Map.of(
            TechnologyStatus.CONFIRMED_EXPERIENCE, 1.0,
            TechnologyStatus.SEMANTIC_EXPERIENCE, 0.85,
            TechnologyStatus.EXPLICIT_OTHER, 0.75,
            TechnologyStatus.SKILLS_ONLY, 0.5,
            TechnologyStatus.MISSING, 0.0,
            TechnologyStatus.IRRELEVANT, 0.0);

    public static final int FILTER_MATCH = 100;
    public static final int FILTER_PARTIAL = 60;
    public static final int FILTER_MISMATCH = 0;
    public static final int NO_KNOWN_FILTERS_SCORE = 50;

    public static final double HH_SEARCH_MATCH_WEIGHT = 0.20;
    public static final double HH_STRUCTURED_FILTERS_WEIGHT = 0.20;
    public static final double VACANCY_MATCH_WEIGHT = 0.25;
    public static final double KEYWORD_COVERAGE_WEIGHT = 0.20;
    public static final double RECRUITER_READABILITY_WEIGHT = 0.15;

    public static final double VACANCY_SKILL_COVERAGE_WEIGHT = 0.80;
    public static final double TARGET_LEVEL_FIT_WEIGHT = 0.20;

    public static final Map<String, TechnologyTier> TECHNOLOGY_TIERS = technologyTiers();

    private static final Map<String, String> SKILL_ALIASES = Map.ofEntries(
            Map.entry("java 8", "Java"), Map.entry("java 11", "Java"), Map.entry("java 17", "Java"),
            Map.entry("java 21", "Java"), Map.entry("java 25", "Java"), Map.entry("java se", "Java"),
            Map.entry("java ee", "Java"), Map.entry("javase", "Java"), Map.entry("javaee", "Java"),
            Map.entry("spring boot", "Spring Boot"), Map.entry("springboot", "Spring Boot"),
            Map.entry("spring mvc", "Spring MVC"), Map.entry("spring security", "Spring Security"),
            Map.entry("spring data", "Spring Data"), Map.entry("spring cloud", "Spring Cloud"),
            Map.entry("spring framework", "Spring"), Map.entry("postgresql", "PostgreSQL"),
            Map.entry("postgres", "PostgreSQL"), Map.entry("postgre sql", "PostgreSQL"),
            Map.entry("mysql", "MySQL"), Map.entry("mongo db", "MongoDB"), Map.entry("mongodb", "MongoDB"),
            Map.entry("redis cache", "Redis"), Map.entry("redis db", "Redis"), Map.entry("kafka", "Kafka"),
            Map.entry("apache kafka", "Kafka"), Map.entry("rabbit mq", "RabbitMQ"), Map.entry("rabbitmq", "RabbitMQ"),
            Map.entry("docker container", "Docker"), Map.entry("dockerhub", "Docker"), Map.entry("k8s", "Kubernetes"),
            Map.entry("kubernetes cluster", "Kubernetes"), Map.entry("linux os", "Linux"),
            Map.entry("linux server", "Linux"), Map.entry("ubuntu", "Linux"), Map.entry("centos", "Linux"),
            Map.entry("debian", "Linux"), Map.entry("ci cd", "CI/CD"),
            Map.entry("continuous integration", "CI/CD"), Map.entry("continuous deployment", "CI/CD"),
            Map.entry("jenkins", "Jenkins"), Map.entry("gitlab ci", "GitLab CI"), Map.entry("gitlabci", "GitLab CI"),
            Map.entry("github actions", "GitHub Actions"), Map.entry("githubactions", "GitHub Actions"),
            Map.entry("aws", "AWS"), Map.entry("amazon web services", "AWS"), Map.entry("azure", "Azure"),
            Map.entry("microsoft azure", "Azure"), Map.entry("gcp", "GCP"), Map.entry("google cloud", "GCP"),
            Map.entry("google cloud platform", "GCP"), Map.entry("junit 5", "JUnit"), Map.entry("junit 4", "JUnit"),
            Map.entry("mockito mock", "Mockito"), Map.entry("mockito core", "Mockito"),
            Map.entry("test container", "Testcontainers"), Map.entry("testcontainers", "Testcontainers"),
            Map.entry("git version", "Git"), Map.entry("git scm", "Git"), Map.entry("maven build", "Maven"),
            Map.entry("gradle build", "Gradle"), Map.entry("intellij", "IntelliJ IDEA"),
            Map.entry("intellij idea", "IntelliJ IDEA"), Map.entry("eclipse", "Eclipse"),
            Map.entry("vscode", "VS Code"), Map.entry("visual studio code", "VS Code"),
            Map.entry("rest api", "REST API"), Map.entry("restful", "REST API"),
            Map.entry("graphql", "GraphQL"), Map.entry("grpc", "gRPC"));

    private static final List<Alias> CANONICAL_ALIASES = List.of(
            alias("^java(?:\\s+\\d+)?$", "Java"),
            alias("spring\\s*boot", "Spring Boot"),
            alias("^(?:spring|spring framework)$", "Spring Boot"),
            alias("(?:rest(?:ful)?(?:\\s*api|\\s*endpoint|\\s*service)?|web api)", "REST API"),
            alias("postgres", "PostgreSQL"),
            alias("^(?:sql|mysql|oracle|mariadb|relational database)$", "SQL"),
            alias("(?:hibernate|\\bjpa\\b)", "Hibernate/JPA"),
            alias("(?:maven|gradle)", "Maven/Gradle"),
            alias("(?:junit|mockito|testcontainers|unit test|integration test)", "Testing"),
            alias("spring\\s*data", "Spring Data"),
            alias("spring\\s*security", "Spring Security"),
            alias("microservice", "Microservices"),
            alias("(?:ci/?cd|continuous integration)", "CI/CD"),
            alias("backend", "Backend development"));

    private AtsScoringProperties() {
    }

    public static String canonicalTechnology(String value) {
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String normalized = SKILL_ALIASES.getOrDefault(lower,
                lower.isEmpty() ? lower : lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1));
        for (Alias alias : CANONICAL_ALIASES) {
            if (alias.pattern().matcher(normalized).find()) {
                return alias.canonical();
            }
        }
        return TECHNOLOGY_TIERS.keySet().stream()
                .filter(technology -> technology.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseGet(() -> normalized.isEmpty()
                        ? normalized
                        : normalized.substring(0, 1).toUpperCase() + normalized.substring(1).toLowerCase());
    }

    private static Map<String, TechnologyTier> technologyTiers() {
        Map<String, TechnologyTier> tiers = new LinkedHashMap<>();
        tiers.put("Java", TechnologyTier.CORE);
        tiers.put("Spring Boot", TechnologyTier.CORE);
        tiers.put("REST API", TechnologyTier.CORE);
        tiers.put("SQL", TechnologyTier.CORE);
        tiers.put("PostgreSQL", TechnologyTier.CORE);
        tiers.put("Git", TechnologyTier.CORE);
        tiers.put("Backend development", TechnologyTier.CORE);
        tiers.put("Hibernate/JPA", TechnologyTier.COMMON);
        tiers.put("Docker", TechnologyTier.COMMON);
        tiers.put("Testing", TechnologyTier.COMMON);
        tiers.put("Maven/Gradle", TechnologyTier.COMMON);
        tiers.put("Spring Data", TechnologyTier.COMMON);
        tiers.put("Spring Security", TechnologyTier.COMMON);
        tiers.put("Kafka", TechnologyTier.BONUS);
        tiers.put("RabbitMQ", TechnologyTier.BONUS);
        tiers.put("Redis", TechnologyTier.BONUS);
        tiers.put("Kubernetes", TechnologyTier.BONUS);
        tiers.put("Microservices", TechnologyTier.BONUS);
        tiers.put("Prometheus", TechnologyTier.BONUS);
        tiers.put("Grafana", TechnologyTier.BONUS);
        tiers.put("Linux", TechnologyTier.BONUS);
        tiers.put("CI/CD", TechnologyTier.BONUS);
        return Collections.unmodifiableMap(tiers);
    }

    private static Alias alias(String expression, String canonical) {
        return new Alias(Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), canonical);
    }

    private record Alias(Pattern pattern, String canonical) {
    }
}
