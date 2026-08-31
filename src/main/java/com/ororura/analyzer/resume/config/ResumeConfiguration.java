package com.ororura.analyzer.resume.config;

import java.time.Clock;

import com.ororura.analyzer.codex.CodexCliProperties;
import com.ororura.analyzer.polza.PolzaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ResumeAnalysisProperties.class, ResumeScoringProperties.class, AiProperties.class,
        CodexCliProperties.class, PolzaProperties.class})
public class ResumeConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
