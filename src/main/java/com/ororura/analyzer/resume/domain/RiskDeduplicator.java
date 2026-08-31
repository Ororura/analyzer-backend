package com.ororura.analyzer.resume.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import com.ororura.analyzer.resume.api.ResumeRisk;
import org.springframework.stereotype.Component;

@Component
public class RiskDeduplicator {
    public List<ResumeRisk> deduplicate(List<ResumeRisk> risks) {
        LinkedHashMap<String, ResumeRisk> result = new LinkedHashMap<>();
        for (ResumeRisk risk : risks) {
            String key = risk.type() + ":" + normalize(risk.subjectKey() == null ? risk.title() : risk.subjectKey());
            result.merge(key, risk, RiskDeduplicator::stronger);
        }
        return List.copyOf(result.values());
    }

    private static ResumeRisk stronger(ResumeRisk first, ResumeRisk second) {
        return second.severity().ordinal() < first.severity().ordinal() ? second : first;
    }

    private static String normalize(String value) {
        return value == null ? "unknown" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
