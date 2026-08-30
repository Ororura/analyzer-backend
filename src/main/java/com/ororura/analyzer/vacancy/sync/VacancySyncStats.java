package com.ororura.analyzer.vacancy.sync;

public record VacancySyncStats(
        int fetched,
        int created,
        int updated,
        int unchanged,
        int archived,
        int failed,
        long durationMillis) {
}
