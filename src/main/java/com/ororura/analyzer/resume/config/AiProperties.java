package com.ororura.analyzer.resume.config;

import com.ororura.analyzer.resume.ai.AiProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public record AiProperties(AiProviderType defaultProvider) {
}
