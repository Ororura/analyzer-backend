package com.ororura.analyzer.polza;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.polza")
public record PolzaProperties(
        URI baseUrl,
        String model,
        String apiKey,
        Duration timeout) {
}
