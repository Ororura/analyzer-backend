package com.ororura.analyzer.market;

import com.ororura.analyzer.market.domain.MarketAnalysisProfileFactory;
import com.ororura.analyzer.market.domain.VacancyMarketAggregator;
import com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy;
import com.ororura.analyzer.analysis.profile.AnalysisTechnologyCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MarketConfiguration {

    @Bean
    MarketAnalysisProfileFactory marketAnalysisProfileFactory() {
        return new MarketAnalysisProfileFactory();
    }

    @Bean
    VacancyMarketAggregator vacancyMarketAggregator(
            AnalysisTechnologyCatalog technologyCatalog, AnalysisScoringPolicy scoringPolicy) {
        return new VacancyMarketAggregator(technologyCatalog, scoringPolicy);
    }
}
