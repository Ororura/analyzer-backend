package com.ororura.analyzer.resume.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.domain.MarketRequirement;

public class TechnologyTaxonomy {

    public enum Evidence { CONFIRMED, WEAK, EXPLICIT, MISSING }

    public TechnologyProfile profile(MarketAnalysisProfile profile, String text,
            List<String> confirmed, List<String> weak, List<String> missing) {
        Map<String, Evidence> evidence = new LinkedHashMap<>();
        add(profile.requirements(), evidence, missing, Evidence.MISSING);
        explicitlyPresent(profile.requirements(), text).forEach(name -> evidence.put(name, Evidence.EXPLICIT));
        add(profile.requirements(), evidence, weak, Evidence.WEAK);
        add(profile.requirements(), evidence, confirmed, Evidence.CONFIRMED);
        return new TechnologyProfile(Collections.unmodifiableMap(evidence),
                names(evidence, Evidence.CONFIRMED),
                names(evidence, Evidence.WEAK, Evidence.EXPLICIT), names(evidence, Evidence.MISSING));
    }

    private static Set<String> explicitlyPresent(List<MarketRequirement> requirements, String text) {
        Set<String> result = new LinkedHashSet<>();
        String source = text == null ? "" : text;
        for (MarketRequirement requirement : requirements) {
            Pattern pattern = Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(requirement.label())
                    + "(?![\\p{L}\\p{N}])");
            if (pattern.matcher(source).find()) result.add(requirement.label());
        }
        return Collections.unmodifiableSet(result);
    }

    private static void add(List<MarketRequirement> requirements, Map<String, Evidence> target,
            List<String> values, Evidence status) {
        if (values == null) return;
        for (String value : values) {
            requirements.stream().map(MarketRequirement::label)
                    .filter(label -> label.equalsIgnoreCase(value)).findFirst()
                    .ifPresent(label -> target.put(label, status));
        }
    }

    private static List<String> names(Map<String, Evidence> values, Evidence... statuses) {
        Set<Evidence> included = Set.of(statuses);
        List<String> result = new ArrayList<>();
        values.forEach((name, value) -> { if (included.contains(value)) result.add(name); });
        return List.copyOf(result);
    }

    public record TechnologyProfile(
            Map<String, Evidence> evidence,
            List<String> confirmed,
            List<String> weakEvidence,
            List<String> missing) {
    }
}
