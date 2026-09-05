package com.ororura.analyzer.analysis.domain;

import java.time.Instant;
import java.util.*;
import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics;
import com.ororura.analyzer.vacancy.requirement.generation.GeneratedCriterionGroup;

/** Unweighted market facts; no user priorities or scoring coefficients. */
public record MarketSnapshot(UUID id, MarketSegment segment, Instant generatedAt, String pipelineVersion, Map<String,String> provenance,
        List<String> queries, int scannedCount, int uniqueCount, boolean truncated,
        List<SnapshotVacancy> vacancies, MarketRequirementStatistics statistics,
        List<GeneratedCriterionGroup> criteria, List<String> warnings) {
    public MarketSnapshot {
        provenance = Map.copyOf(provenance);
        queries = List.copyOf(queries); vacancies = List.copyOf(vacancies);
        criteria = List.copyOf(criteria); warnings = List.copyOf(warnings);
    }
    public record SnapshotVacancy(Vacancy vacancy, CandidateGrade vacancyGrade, String gradeSource) {}
}
