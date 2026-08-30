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

import com.ororura.analyzer.resume.ai.AnalysisTechnology;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileDefinition;
import org.springframework.stereotype.Component;

@Component
public class TechnologyTaxonomy {

    public enum Evidence { CONFIRMED, WEAK, EXPLICIT, MISSING }

    public String canonical(ResumeAnalysisProfileDefinition profile, String value) {
        return canonical(profile.technologies(), value);
    }

    public String canonical(List<AnalysisTechnology> technologies, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return normalized;
        String exact = technologies.stream().map(AnalysisTechnology::name)
                .filter(name -> name.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
        if (exact != null) return exact;
        for (Alias alias : aliases(technologies)) {
            if (alias.pattern().matcher(normalized).find()) return alias.canonical();
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalized.substring(1).toLowerCase(Locale.ROOT);
    }

    public Set<String> explicitlyPresent(ResumeAnalysisProfileDefinition profile, String text) {
        Set<String> result = new LinkedHashSet<>();
        String source = text == null ? "" : text;
        for (Alias alias : aliases(profile)) {
            if (alias.pattern().matcher(source).find()) result.add(alias.canonical());
        }
        return Collections.unmodifiableSet(result);
    }

    public TechnologyProfile profile(ResumeAnalysisProfileDefinition definition, String text,
            List<String> confirmed, List<String> weak, List<String> missing) {
        Map<String, Evidence> evidence = new LinkedHashMap<>();
        add(definition, evidence, missing, Evidence.MISSING);
        explicitlyPresent(definition, text).forEach(name -> evidence.put(name, Evidence.EXPLICIT));
        add(definition, evidence, weak, Evidence.WEAK);
        add(definition, evidence, confirmed, Evidence.CONFIRMED);
        return new TechnologyProfile(Collections.unmodifiableMap(evidence),
                names(evidence, Evidence.CONFIRMED),
                names(evidence, Evidence.WEAK, Evidence.EXPLICIT), names(evidence, Evidence.MISSING));
    }

    public Map<String, AnalysisTechnology.Tier> tiers(ResumeAnalysisProfileDefinition profile) {
        return tiers(profile.technologies());
    }

    public Map<String, AnalysisTechnology.Tier> tiers(List<AnalysisTechnology> technologies) {
        return technologies.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                AnalysisTechnology::name, AnalysisTechnology::tier));
    }

    private void add(ResumeAnalysisProfileDefinition profile, Map<String, Evidence> target,
            List<String> values, Evidence status) {
        if (values == null) return;
        for (String value : values) {
            String canonical = canonical(profile, value);
            if (!canonical.isBlank()) target.put(canonical, status);
        }
    }

    private static List<String> names(Map<String, Evidence> values, Evidence... statuses) {
        Set<Evidence> included = Set.of(statuses);
        List<String> result = new ArrayList<>();
        values.forEach((name, value) -> { if (included.contains(value)) result.add(name); });
        return List.copyOf(result);
    }

    private static List<Alias> aliases(ResumeAnalysisProfileDefinition profile) {
        return aliases(profile.technologies());
    }

    private static List<Alias> aliases(List<AnalysisTechnology> technologies) {
        return technologies.stream()
                .flatMap(technology -> technology.patterns().stream()
                        .map(pattern -> new Alias(Pattern.compile(pattern,
                                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), technology.name())))
                .toList();
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
