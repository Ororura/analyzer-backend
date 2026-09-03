package com.ororura.analyzer.vacancy.application.port;

import java.util.List;

public record VacancySyncBatchResult(int created, int updated, int unchanged, List<String> changedExternalIds,
        Long marketVersion) {
}
