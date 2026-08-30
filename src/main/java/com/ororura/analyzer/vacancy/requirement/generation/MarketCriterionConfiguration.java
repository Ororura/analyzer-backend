package com.ororura.analyzer.vacancy.requirement.generation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MarketCriterionProperties.class)
class MarketCriterionConfiguration {
}
