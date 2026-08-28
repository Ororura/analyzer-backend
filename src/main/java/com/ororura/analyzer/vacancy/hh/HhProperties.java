package com.ororura.analyzer.vacancy.hh;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.hh")
public record HhProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
}
