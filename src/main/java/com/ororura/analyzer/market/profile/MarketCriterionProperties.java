package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.resume.market.criteria")
public record MarketCriterionProperties(int minimumCount, int maximumCount) {

    public MarketCriterionProperties {
        if (minimumCount < 1 || maximumCount < minimumCount) {
            throw new IllegalArgumentException("Market criterion count range is invalid");
        }
    }
}
