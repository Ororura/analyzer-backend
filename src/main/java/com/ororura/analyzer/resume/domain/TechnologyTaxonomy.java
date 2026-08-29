package com.ororura.analyzer.resume.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TechnologyTaxonomy {

    public enum Tier { CORE, COMMON, BONUS }
    public enum Evidence { CONFIRMED, WEAK, EXPLICIT, MISSING }

    private static final Map<String, Tier> TIERS = createTiers();
    private static final List<Alias> ALIASES = List.of(
            alias("\\bjava(?:\\s+(?:8|11|17|21|25))?\\b", "Java"),
            alias("\\bspring\\s*boot\\b|\\bspring framework\\b|\\bspring\\b", "Spring Boot"),
            alias("\\brest(?:ful)?(?:\\s+api|\\s+endpoints?|\\s+services?)?\\b|\\bweb api\\b", "REST API"),
            alias("\\bpostgres(?:ql)?\\b", "PostgreSQL"),
            alias("\\b(?:sql|mysql|oracle|mariadb)\\b", "SQL"),
            alias("\\bhibernate\\b|\\bjpa\\b|jakarta persistence", "Hibernate/JPA"),
            alias("\\bmaven\\b|\\bgradle\\b", "Maven/Gradle"),
            alias("\\bjunit\\b|\\bmockito\\b|\\btestcontainers?\\b|unit test|integration test", "Testing"),
            alias("spring\\s+data", "Spring Data"),
            alias("spring\\s+security", "Spring Security"),
            alias("\\bkafka\\b|apache kafka", "Kafka"),
            alias("rabbit\\s*mq", "RabbitMQ"),
            alias("\\bredis\\b", "Redis"),
            alias("\\bk8s\\b|\\bkubernetes\\b", "Kubernetes"),
            alias("microservices?", "Microservices"),
            alias("prometheus", "Prometheus"),
            alias("grafana", "Grafana"),
            alias("\\blinux\\b|\\bubuntu\\b|\\bcentos\\b|\\bdebian\\b", "Linux"),
            alias("ci\\s*/?\\s*cd|continuous integration|gitlab ci(?:/cd)?|github actions|jenkins", "CI/CD"),
            alias("\\bgit\\b", "Git"),
            alias("backend(?: development)?", "Backend development"),
            alias("\\bdocker\\b", "Docker"));

    public String canonical(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return normalized;
        String exact = TIERS.keySet().stream()
                .filter(name -> name.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
        if (exact != null) return exact;
        for (Alias alias : ALIASES) {
            if (alias.pattern().matcher(normalized).find()) return alias.canonical();
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalized.substring(1).toLowerCase(Locale.ROOT);
    }

    public Set<String> explicitlyPresent(String text) {
        Set<String> result = new LinkedHashSet<>();
        String source = text == null ? "" : text;
        for (Alias alias : ALIASES) {
            if (alias.pattern().matcher(source).find()) result.add(alias.canonical());
        }
        return Collections.unmodifiableSet(result);
    }

    public TechnologyProfile profile(String text, List<String> confirmed, List<String> weak, List<String> missing) {
        Map<String, Evidence> evidence = new LinkedHashMap<>();
        add(evidence, missing, Evidence.MISSING);
        explicitlyPresent(text).forEach(name -> evidence.put(name, Evidence.EXPLICIT));
        add(evidence, weak, Evidence.WEAK);
        add(evidence, confirmed, Evidence.CONFIRMED);
        return new TechnologyProfile(Collections.unmodifiableMap(evidence),
                names(evidence, Evidence.CONFIRMED),
                names(evidence, Evidence.WEAK, Evidence.EXPLICIT), names(evidence, Evidence.MISSING));
    }

    public Map<String, Tier> tiers() {
        return TIERS;
    }

    private void add(Map<String, Evidence> target, List<String> values, Evidence status) {
        if (values == null) return;
        for (String value : values) {
            String canonical = canonical(value);
            if (!canonical.isBlank()) target.put(canonical, status);
        }
    }

    private static List<String> names(Map<String, Evidence> values, Evidence... statuses) {
        Set<Evidence> included = Set.of(statuses);
        List<String> result = new ArrayList<>();
        values.forEach((name, value) -> { if (included.contains(value)) result.add(name); });
        return List.copyOf(result);
    }

    private static Alias alias(String expression, String canonical) {
        return new Alias(Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), canonical);
    }

    private static Map<String, Tier> createTiers() {
        Map<String, Tier> tiers = new LinkedHashMap<>();
        List.of("Java", "Spring Boot", "REST API", "SQL", "PostgreSQL", "Git", "Backend development")
                .forEach(name -> tiers.put(name, Tier.CORE));
        List.of("Hibernate/JPA", "Docker", "Testing", "Maven/Gradle", "Spring Data", "Spring Security")
                .forEach(name -> tiers.put(name, Tier.COMMON));
        List.of("Kafka", "RabbitMQ", "Redis", "Kubernetes", "Microservices", "Prometheus", "Grafana", "Linux", "CI/CD")
                .forEach(name -> tiers.put(name, Tier.BONUS));
        return Collections.unmodifiableMap(tiers);
    }

    private record Alias(Pattern pattern, String canonical) {
    }

    public record TechnologyProfile(
            Map<String, Evidence> evidence,
            List<String> confirmed,
            List<String> weakEvidence,
            List<String> missing) {
    }
}
