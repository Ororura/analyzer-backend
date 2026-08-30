package com.ororura.analyzer.resume.domain;

import java.util.List;
import java.util.Map;

import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtsScoreCalculatorTests {

    private final TechnologyTaxonomy taxonomy = new TechnologyTaxonomy();
    private final AtsScoreCalculator calculator = new AtsScoreCalculator(taxonomy);
    private final JavaBackendAnalysisProfile definition = new JavaBackendAnalysisProfile();

    @Test
    void clampsMinimumMaximumAndIsDeterministic() {
        var none = taxonomy.profile(definition, "", List.of(), List.of(), List.of());
        var all = taxonomy.profile(definition, "", taxonomy.tiers(definition).keySet().stream().toList(), List.of(), List.of());
        VacancyMarketData market = market();
        assertThat(calculator.calculate(0, 0, 0, 0, none, market, definition).total()).isZero();
        assertThat(calculator.calculate(10, 10, 10, 10, all, market, definition).total()).isEqualTo(100);
        assertThat(calculator.calculate(8, 7, 6, 7, all, market, definition))
                .isEqualTo(calculator.calculate(8, 7, 6, 7, all, market, definition));
    }

    private VacancyMarketData market() {
        return new VacancyMarketData("test", 1,
                taxonomy.tiers(definition).keySet().stream().collect(java.util.stream.Collectors.toMap(
                        value -> value, value -> 1.0)), Map.of(), Map.of(), Map.of(), List.of());
    }
}
