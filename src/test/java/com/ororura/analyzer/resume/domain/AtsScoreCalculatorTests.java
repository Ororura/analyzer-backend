package com.ororura.analyzer.resume.domain;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtsScoreCalculatorTests {

    private final TechnologyTaxonomy taxonomy = new TechnologyTaxonomy();
    private final AtsScoreCalculator calculator = new AtsScoreCalculator();
    private final com.ororura.analyzer.resume.market.MarketAnalysisProfile profile =
            TechnologyTaxonomyTests.profile();

    @Test
    void clampsMinimumMaximumAndIsDeterministic() {
        var none = taxonomy.profile(profile, "", List.of(), List.of(), List.of());
        var all = taxonomy.profile(profile, "", profile.requirements().stream().map(value -> value.label()).toList(),
                List.of(), List.of());
        assertThat(calculator.calculate(0, 0, 0, 0, none, profile).total()).isZero();
        assertThat(calculator.calculate(10, 10, 10, 10, all, profile).total()).isEqualTo(100);
        assertThat(calculator.calculate(8, 7, 6, 7, all, profile))
                .isEqualTo(calculator.calculate(8, 7, 6, 7, all, profile));
    }
}
