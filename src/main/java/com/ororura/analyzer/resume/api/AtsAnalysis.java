package com.ororura.analyzer.resume.api;

import java.util.List;

public record AtsAnalysis(int score, int parsing, int sections, int contacts, int experience,
        int education, int skills, int keywordCoverage, List<FieldDiagnostic> diagnostics,
        ScoreBreakdown breakdown) {
    public AtsAnalysis { diagnostics = List.copyOf(diagnostics); }
    public record FieldDiagnostic(String field, FieldStatus status, List<String> issues) {
        public FieldDiagnostic { issues = List.copyOf(issues); }
    }
    public enum FieldStatus { OK, PARTIAL, FAILED }
}
