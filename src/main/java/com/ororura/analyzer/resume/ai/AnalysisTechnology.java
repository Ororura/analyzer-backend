package com.ororura.analyzer.resume.ai;

import java.util.List;

public record AnalysisTechnology(String name, Tier tier, List<String> patterns) {

    public AnalysisTechnology {
        if (name == null || name.isBlank() || tier == null || patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("Analysis technology must define name, tier and patterns");
        }
        patterns = List.copyOf(patterns);
    }

    public enum Tier { CORE, COMMON, BONUS }
}
