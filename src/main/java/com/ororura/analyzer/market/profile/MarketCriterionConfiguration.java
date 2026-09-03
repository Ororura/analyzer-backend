package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MarketCriterionProperties.class)
class MarketCriterionConfiguration {
}
