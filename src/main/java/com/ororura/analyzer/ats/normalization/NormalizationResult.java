package com.ororura.analyzer.ats.normalization;

import java.util.List;

import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis;

public record NormalizationResult(NormalizedAtsAnalysis result, List<NormalizationDiagnostic> diagnostics) {
    public NormalizationResult {
        diagnostics = List.copyOf(diagnostics);
    }
}
