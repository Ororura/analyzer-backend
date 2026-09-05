package com.ororura.analyzer.analysis.domain;

import java.util.*;

public record MarketSegment(CareerDirection direction, String specialization, CandidateGrade targetGrade,
        List<String> technologies, MarketFilters filters, CandidateGrade searchGrade) {
    public MarketSegment {
        Objects.requireNonNull(direction); Objects.requireNonNull(targetGrade);
        Objects.requireNonNull(filters); Objects.requireNonNull(searchGrade);
        if (specialization == null || specialization.isBlank()) throw new IllegalArgumentException("specialization is required");
        specialization = specialization.trim();
        technologies = technologies.stream().map(String::trim).map(s -> s.toLowerCase(Locale.ROOT)).distinct().sorted().toList();
    }
    public String targetRole() { return specialization + " " + direction.name() + " Developer"; }
}
