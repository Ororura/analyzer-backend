package com.ororura.analyzer.integration.codex;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai.codex")
public record CodexCliProperties(
        boolean enabled,
        String executable,
        String model,
        Duration timeout,
        Duration availabilityTimeout,
        int maxConcurrentProcesses) {

    public CodexCliProperties {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("Codex executable is required");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Codex timeout must be positive");
        }
        if (availabilityTimeout == null || availabilityTimeout.isZero() || availabilityTimeout.isNegative()) {
            throw new IllegalArgumentException("Codex availability timeout must be positive");
        }
        if (maxConcurrentProcesses < 1) {
            throw new IllegalArgumentException("Codex max concurrent processes must be positive");
        }
    }
}
