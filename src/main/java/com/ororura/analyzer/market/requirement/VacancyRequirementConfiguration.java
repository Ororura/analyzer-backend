package com.ororura.analyzer.market.requirement;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MarketRequirementProperties.class)
class VacancyRequirementConfiguration {
}
