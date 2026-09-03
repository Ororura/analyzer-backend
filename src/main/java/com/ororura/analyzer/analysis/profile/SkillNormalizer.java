package com.ororura.analyzer.analysis.profile;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.ororura.analyzer.analysis.profile.AnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;

public class SkillNormalizer {
    private static final Pattern SEPARATORS = Pattern.compile("[\\s_-]+");
    private final AnalysisTechnologyCatalog catalog;

    public SkillNormalizer(AnalysisTechnologyCatalog catalog) {
        this.catalog = catalog;
    }

    public String normalizeId(ResumeAnalysisProfile profile, String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = key(value);
        for (SkillDefinition definition : definitions(profile)) {
            if (key(definition.displayName()).equals(normalized)
                    || definition.aliases().stream().map(SkillNormalizer::key).anyMatch(normalized::equals)) {
                return definition.id();
            }
        }
        for (var technology : catalog.technologies(profile)) {
            if (technology.patterns().stream().anyMatch(expression -> Pattern.compile(expression,
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(value.trim()).matches())) {
                return key(technology.name()).replace(' ', '_');
            }
        }
        return normalized.replace(' ', '_');
    }

    public String displayName(ResumeAnalysisProfile profile, String value) {
        String id = normalizeId(profile, value);
        return definitions(profile).stream().filter(item -> item.id().equals(id))
                .map(SkillDefinition::displayName).findFirst().orElseGet(() -> displayUnknown(value));
    }

    public List<SkillDefinition> definitions(ResumeAnalysisProfile profile) {
        Map<String, SkillDefinition> definitions = new LinkedHashMap<>();
        catalog.technologies(profile).forEach(technology -> {
            String id = key(technology.name()).replace(' ', '_');
            List<String> aliases = technology.patterns().stream()
                    .filter(expression -> expression.matches("[\\p{L}\\p{N} ._+/#-]+"))
                    .toList();
            definitions.put(id, new SkillDefinition(id, technology.name(), aliases, parent(id)));
        });
        add(definitions, "spring", "Spring", null, "spring framework");
        add(definitions, "spring_boot", "Spring Boot", "spring", "springboot", "spring-boot");
        add(definitions, "spring_security", "Spring Security", "spring", "spring-security");
        add(definitions, "spring_data", "Spring Data", "spring", "spring-data", "spring data jpa");
        add(definitions, "postgresql", "PostgreSQL", null, "postgres");
        add(definitions, "kubernetes", "Kubernetes", null, "k8s");
        return List.copyOf(definitions.values());
    }

    private static void add(Map<String, SkillDefinition> target, String id, String name, String parent,
            String... aliases) {
        target.put(id, new SkillDefinition(id, name, List.of(aliases), parent));
    }

    private static String parent(String id) {
        return id.startsWith("spring_") ? "spring" : null;
    }

    private static String key(String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        return SEPARATORS.matcher(normalized).replaceAll(" ").trim();
    }

    private static String displayUnknown(String value) {
        if (value == null || value.isBlank()) return "Unknown";
        String normalized = value.trim();
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
