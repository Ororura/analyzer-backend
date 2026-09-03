package com.ororura.analyzer.analysis.profile;

import java.util.List;
import java.util.Map;


/** Deterministic alias/regex catalog used as an extraction fast path, never as market statistics. */
public class DefaultAnalysisTechnologyCatalog implements AnalysisTechnologyCatalog {

    private static final Map<ResumeAnalysisProfile, List<AnalysisTechnology>> CATALOG = Map.of(
            ResumeAnalysisProfile.JAVA_BACKEND, List.of(
                    core("Java", "\\bjava(?:\\s+(?:8|11|17|21|25))?\\b"),
                    core("Spring Boot", "\\bspring\\s*boot\\b|\\bspring framework\\b|\\bspring\\b"),
                    core("REST API", "\\brest(?:ful)?(?:\\s+api|\\s+endpoints?|\\s+services?)?\\b|\\bweb api\\b"),
                    core("PostgreSQL", "\\bpostgres(?:ql)?\\b"), core("SQL", "\\b(?:sql|mysql|oracle|mariadb)\\b"),
                    core("Git", "\\bgit\\b"), core("Backend development", "backend(?: development)?"),
                    tech("Hibernate/JPA", "\\bhibernate\\b|\\bjpa\\b|jakarta persistence"),
                    tech("Docker", "\\bdocker\\b"),
                    tech("Testing", "\\bjunit\\b|\\bmockito\\b|\\btestcontainers?\\b|unit test|integration test"),
                    tech("Maven/Gradle", "\\bmaven\\b|\\bgradle\\b"), tech("Spring Data", "spring\\s+data"),
                    tech("Spring Security", "spring\\s+security"), bonus("Kafka", "\\bkafka\\b|apache kafka"),
                    bonus("RabbitMQ", "rabbit\\s*mq"), bonus("Redis", "\\bredis\\b"),
                    bonus("Kubernetes", "\\bk8s\\b|\\bkubernetes\\b"), bonus("Microservices", "microservices?"),
                    bonus("Prometheus", "prometheus"), bonus("Grafana", "grafana"),
                    bonus("Linux", "\\blinux\\b|\\bubuntu\\b|\\bcentos\\b|\\bdebian\\b"),
                    bonus("CI/CD", "ci\\s*/?\\s*cd|continuous integration|gitlab ci(?:/cd)?|github actions|jenkins")),
            ResumeAnalysisProfile.REACT_FRONTEND, List.of(
                    tech("JavaScript", "\\bjavascript\\b|\\bjs\\b"), tech("TypeScript", "\\btypescript\\b|\\bts\\b"),
                    tech("React", "\\breact(?:\\.js|js)?\\b"), tech("HTML", "\\bhtml5?\\b"),
                    tech("CSS", "\\bcss3?\\b"), tech("REST API", "\\brest(?:ful)?(?:\\s+api)?\\b|\\bweb api\\b"),
                    tech("Git", "\\bgit\\b"), tech("Frontend development", "front[ -]?end(?: development)?"),
                    tech("State management", "state management|react context|\\bredux(?: toolkit)?\\b|\\bzustand\\b|\\bmobx\\b"),
                    tech("Testing", "\\bjest\\b|\\bvitest\\b|react testing library|\\bplaywright\\b|\\bcypress\\b|unit test|integration test|e2e"),
                    tech("Responsive UI", "responsive|adaptive layout|адаптив"),
                    tech("Accessibility", "accessibility|\\ba11y\\b|доступност"),
                    tech("Next.js", "\\bnext(?:\\.js|js)?\\b"), tech("Tailwind", "\\btailwind(?:css)?\\b"),
                    tech("Sass", "\\bsass\\b|\\bscss\\b"), tech("Webpack/Vite", "\\bwebpack\\b|\\bvite\\b"),
                    tech("Docker", "\\bdocker\\b"), tech("WebSocket", "websockets?|socket\\.io")));

    @Override
    public List<AnalysisTechnology> technologies(ResumeAnalysisProfile profile) {
        return CATALOG.getOrDefault(profile, List.of());
    }

    private static AnalysisTechnology tech(String name, String... patterns) {
        return new AnalysisTechnology(name, AnalysisTechnology.Tier.COMMON, List.of(patterns));
    }

    private static AnalysisTechnology core(String name, String... patterns) {
        return new AnalysisTechnology(name, AnalysisTechnology.Tier.CORE, List.of(patterns));
    }

    private static AnalysisTechnology bonus(String name, String... patterns) {
        return new AnalysisTechnology(name, AnalysisTechnology.Tier.BONUS, List.of(patterns));
    }
}
