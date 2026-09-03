package com.ororura.analyzer.integration.polza;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.polza")
public record PolzaProperties(
        boolean enabled,
        URI baseUrl,
        String model,
        String apiKey,
        Duration timeout) {
}
