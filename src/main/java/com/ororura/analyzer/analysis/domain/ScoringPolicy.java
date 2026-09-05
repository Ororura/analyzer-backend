package com.ororura.analyzer.analysis.domain;

import java.util.Map;

/** Captures every configured coefficient; fixed Java formulas are identified by algorithmVersion. */
public record ScoringPolicy(String algorithmVersion, String gradePolicyVersion, Map<String, Double> parameters) {
    public static final String VERSION = "deterministic-v1";
    public ScoringPolicy { parameters = Map.copyOf(parameters); }
}
