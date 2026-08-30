package com.ororura.analyzer.vacancy.requirement.generation;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CriterionWeightCalculatorTests {

    private final CriterionIdFactory idFactory = new CriterionIdFactory();
    private final CriterionWeightCalculator calculator = new CriterionWeightCalculator(idFactory);

    @Test
    void createsDeterministicCriterionIdFromSortedRequirementIds() {
        String first = idFactory.create("Spring Backend",
                List.of("technology:spring-boot", "practice:rest-api"));
        String reordered = idFactory.create("Spring Backend",
                List.of("practice:rest-api", "technology:spring-boot"));

        assertThat(first).isEqualTo(reordered).startsWith("spring-backend-").hasSize("spring-backend-".length() + 8);
        assertThat(idFactory.create("Spring Backend", List.of("technology:spring-boot")))
                .isNotEqualTo(first);
    }

    @Test
    void calculatesUnionCoverageAndNormalizedWeights() {
        var criteria = calculator.calculate(List.of(
                MarketCriterionFixture.group("Backend", "Java and Spring",
                        "technology:java", "technology:spring-boot"),
                MarketCriterionFixture.group("Databases", "SQL", "technology:sql")),
                MarketCriterionFixture.statistics());

        assertThat(criteria).hasSize(2);
        assertThat(criteria).filteredOn(value -> value.label().equals("Backend"))
                .singleElement().satisfies(value -> {
                    assertThat(value.marketFrequency()).isCloseTo(0.75,
                            org.assertj.core.data.Offset.offset(1e-12));
                    assertThat(value.weight()).isCloseTo(0.75,
                            org.assertj.core.data.Offset.offset(1e-12));
                });
        assertThat(criteria).filteredOn(value -> value.label().equals("Databases"))
                .singleElement().satisfies(value -> {
                    assertThat(value.marketFrequency()).isCloseTo(0.25,
                            org.assertj.core.data.Offset.offset(1e-12));
                    assertThat(value.weight()).isCloseTo(0.25,
                            org.assertj.core.data.Offset.offset(1e-12));
                });
        assertThat(criteria.stream().mapToDouble(value -> value.weight()).sum())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-12));
    }

    @Test
    void keepsZeroCoverageCriterionWithoutBreakingNormalization() {
        var criteria = calculator.calculate(List.of(
                MarketCriterionFixture.group("Backend", "Java", "technology:java"),
                MarketCriterionFixture.group("Static analysis", "SonarQube", "tool:sonarqube")),
                MarketCriterionFixture.statistics());

        assertThat(criteria).filteredOn(value -> value.label().equals("Static analysis"))
                .singleElement().satisfies(value -> {
                    assertThat(value.marketFrequency()).isZero();
                    assertThat(value.weight()).isZero();
                });
        assertThat(criteria.stream().mapToDouble(value -> value.weight()).sum())
                .isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-12));
    }
}
