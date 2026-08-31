package com.ororura.analyzer.resume.api;

import java.util.List;

public record ScoreBreakdown(Integer score, List<ScoreComponent> components) {
    public ScoreBreakdown { components = components == null ? List.of() : List.copyOf(components); }

    public record ScoreComponent(String name, int score, double weight, double contribution, String explanation) {
    }
}
