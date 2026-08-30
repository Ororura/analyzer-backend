package com.ororura.analyzer.vacancy.sync;

import java.util.List;

record VacancySyncBatchResult(int created, int updated, int unchanged, List<String> changedExternalIds,
        Long marketVersion) {
}
