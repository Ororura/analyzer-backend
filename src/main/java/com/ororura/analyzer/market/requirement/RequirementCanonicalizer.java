package com.ororura.analyzer.market.requirement;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.ororura.analyzer.analysis.profile.AnalysisTechnology;
import com.ororura.analyzer.analysis.profile.AnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.market.domain.RequirementType;
import com.ororura.analyzer.market.domain.MarketRequirementId;
import org.springframework.stereotype.Component;

@Component
public class RequirementCanonicalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Map<String, String> SAFE_ALIASES = aliases();
    private final AnalysisTechnologyCatalog technologyCatalog;

    public RequirementCanonicalizer(AnalysisTechnologyCatalog technologyCatalog) {
        this.technologyCatalog = technologyCatalog;
    }

    CanonicalRequirement canonicalize(ResumeAnalysisProfile profile, ExtractedRequirement requirement) {
        String normalized = normalize(requirement.label());
        String canonical = safeAlias(normalized);
        if (canonical == null && (requirement.type() == RequirementType.TECHNOLOGY
                || requirement.type() == RequirementType.TOOL)) {
            canonical = knownTechnology(technologyCatalog.technologies(profile), normalized);
        }
        if (canonical == null) {
            canonical = displayUnknown(normalized);
        }
        String id = MarketRequirementId.of(requirement.type(), canonical);
        return new CanonicalRequirement(id, canonical, requirement.type(), requirement.importance());
    }

    private static String knownTechnology(java.util.List<AnalysisTechnology> technologies, String value) {
        for (AnalysisTechnology technology : technologies) {
            for (String expression : technology.patterns()) {
                if (Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                        .matcher(value).matches()) {
                    return technology.name();
                }
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return WHITESPACE.matcher(Normalizer.normalize(value.trim(), Normalizer.Form.NFKC))
                .replaceAll(" ");
    }

    private static String safeAlias(String value) {
        return SAFE_ALIASES.get(value.toLowerCase(Locale.ROOT));
    }

    private static String displayUnknown(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) return lower;
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
    }

    private static Map<String, String> aliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("postgres", "PostgreSQL");
        aliases.put("postgresql", "PostgreSQL");
        aliases.put("js", "JavaScript");
        aliases.put("javascript", "JavaScript");
        aliases.put("ecmascript", "JavaScript");
        aliases.put("spring", "Spring");
        aliases.put("spring boot", "Spring Boot");
        aliases.put("spring security", "Spring Security");
        aliases.put("spring data", "Spring Data");
        aliases.put("k8s", "Kubernetes");
        aliases.put("kubernetes", "Kubernetes");
        return Map.copyOf(aliases);
    }
}
