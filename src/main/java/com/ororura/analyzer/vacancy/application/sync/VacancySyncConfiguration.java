package com.ororura.analyzer.vacancy.application.sync;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(VacancySyncProperties.class)
class VacancySyncConfiguration {
}
