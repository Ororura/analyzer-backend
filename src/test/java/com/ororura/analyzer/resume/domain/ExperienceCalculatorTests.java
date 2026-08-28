package com.ororura.analyzer.resume.domain;

import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import static com.ororura.analyzer.resume.domain.ExperienceModels.*;
import static org.assertj.core.api.Assertions.assertThat;

class ExperienceCalculatorTests {

    private final ExperienceCalculator calculator = new ExperienceCalculator();

    @Test
    void countsInclusiveMonthBoundaries() {
        assertThat(calculator.calculate(List.of(period("2025-03", "2025-03"))).commercialMonths()).isEqualTo(1);
    }

    @Test
    void mergesOverlappingNestedIdenticalAndAdjacentPeriods() {
        ExperienceSummary result = calculator.calculate(List.of(
                period("2024-01", "2024-06"), period("2024-03", "2024-04"),
                period("2024-01", "2024-06"), period("2024-07", "2024-09")));
        assertThat(result).isEqualTo(new ExperienceSummary(9, 0, 9));
    }

    @Test
    void keepsSeparatedPeriodsAndBuildsYearSummary() {
        ExperienceSummary result = calculator.calculate(List.of(
                period("2023-01", "2023-06"), period("2024-01", "2024-10")));
        assertThat(result).isEqualTo(new ExperienceSummary(16, 1, 4));
    }

    @Test
    void appliesCommercialScoreThresholds() {
        assertThat(List.of(0, 1, 6, 12, 18, 24, 36).stream()
                .map(calculator::commercialScore)).containsExactly(0, 2, 4, 7, 8, 9, 10);
    }

    private static EmploymentPeriod period(String start, String end) {
        return new EmploymentPeriod(YearMonth.parse(start), YearMonth.parse(end));
    }
}
