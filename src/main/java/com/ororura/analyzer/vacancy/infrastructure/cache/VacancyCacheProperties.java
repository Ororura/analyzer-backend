package com.ororura.analyzer.vacancy.infrastructure.cache;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.vacancy.cache")
public record VacancyCacheProperties(Duration detailsTtl, Duration searchTtl) {
}
