package com.ororura.analyzer.vacancy.application.sync;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.vacancy.sync")
public record VacancySyncProperties(
        boolean enabled,
        Duration interval,
        Duration initialDelay,
        String query,
        String area,
        int pagesPerCycle,
        int pageSize,
        int batchSize) {
}
