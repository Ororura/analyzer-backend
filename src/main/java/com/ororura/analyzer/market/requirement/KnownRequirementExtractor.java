package com.ororura.analyzer.market.requirement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ororura.analyzer.analysis.profile.AnalysisTechnology;
import com.ororura.analyzer.analysis.profile.AnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.market.domain.RequirementType;
import org.springframework.stereotype.Component;

@Component
class KnownRequirementExtractor {

    private final AnalysisTechnologyCatalog technologyCatalog;
    private final RequirementCanonicalizer canonicalizer;

    KnownRequirementExtractor(AnalysisTechnologyCatalog technologyCatalog,
            RequirementCanonicalizer canonicalizer) {
        this.technologyCatalog = technologyCatalog;
        this.canonicalizer = canonicalizer;
    }

    List<ExtractedRequirement> extract(ResumeAnalysisProfile profile, VacancyRequirementInput vacancy) {
        String text = vacancy.title() + "\n" + vacancy.description();
        Map<String, ExtractedRequirement> requirements = new LinkedHashMap<>();
        for (AnalysisTechnology technology : technologyCatalog.technologies(profile)) {
            for (String expression : technology.patterns()) {
                Matcher matcher = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                        .matcher(text);
                while (matcher.find()) {
                    ExtractedRequirement candidate = new ExtractedRequirement(
                            matcher.group(), RequirementType.TECHNOLOGY, RequirementImportance.PREFERRED);
                    CanonicalRequirement canonical = canonicalizer.canonicalize(profile, candidate);
                    if (canonical.label().equals(technology.name())) {
                        requirements.putIfAbsent(canonical.id(), new ExtractedRequirement(
                                technology.name(), RequirementType.TECHNOLOGY, RequirementImportance.PREFERRED));
                    }
                }
            }
        }
        return List.copyOf(requirements.values());
    }
}
