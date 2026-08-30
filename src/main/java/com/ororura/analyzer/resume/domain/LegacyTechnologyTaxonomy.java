package com.ororura.analyzer.resume.domain;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.ororura.analyzer.resume.ai.AnalysisTechnology;
import org.springframework.stereotype.Component;

/** Transitional taxonomy for vacancy aggregation and the legacy ATS module. */
@Component
public class LegacyTechnologyTaxonomy {

    public String canonical(List<AnalysisTechnology> technologies, String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return normalized;
        String exact = technologies.stream().map(AnalysisTechnology::name)
                .filter(name -> name.equalsIgnoreCase(normalized)).findFirst().orElse(null);
        if (exact != null) return exact;
        for (Alias alias : aliases(technologies)) {
            if (alias.pattern().matcher(normalized).find()) return alias.canonical();
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT)
                + normalized.substring(1).toLowerCase(Locale.ROOT);
    }

    public Map<String, AnalysisTechnology.Tier> tiers(List<AnalysisTechnology> technologies) {
        return technologies.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                AnalysisTechnology::name, AnalysisTechnology::tier));
    }

    private static List<Alias> aliases(List<AnalysisTechnology> technologies) {
        return technologies.stream().flatMap(technology -> technology.patterns().stream()
                .map(expression -> new Alias(Pattern.compile(expression,
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), technology.name())))
                .toList();
    }

    private record Alias(Pattern pattern, String canonical) {
    }
}
