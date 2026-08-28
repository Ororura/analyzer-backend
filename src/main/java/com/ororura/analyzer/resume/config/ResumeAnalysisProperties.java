package com.ororura.analyzer.resume.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app.resume")
public record ResumeAnalysisProperties(
        DataSize maxFileSize,
        String targetRole,
        String analysisVersion,
        String baselineVersion,
        int maxTextLength) {
}
