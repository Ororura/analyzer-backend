package com.ororura.analyzer.resume.config;

import com.ororura.analyzer.resume.application.port.AiProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public record AiProperties(AiProviderType defaultProvider) {
}
