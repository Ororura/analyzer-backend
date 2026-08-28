package com.ororura.analyzer.resume.domain;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import static com.ororura.analyzer.resume.domain.ExperienceModels.*;

@Component
public class ExperienceCalculator {

    public ExperienceSummary calculate(List<EmploymentPeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return new ExperienceSummary(0, 0, 0);
        }
        List<EmploymentPeriod> sorted = periods.stream()
                .sorted(Comparator.comparing(EmploymentPeriod::start).thenComparing(EmploymentPeriod::end))
                .toList();
        List<EmploymentPeriod> merged = new ArrayList<>();
        for (EmploymentPeriod candidate : sorted) {
            if (merged.isEmpty()) {
                merged.add(candidate);
                continue;
            }
            EmploymentPeriod previous = merged.getLast();
            if (!candidate.start().isAfter(previous.end().plusMonths(1))) {
                merged.set(merged.size() - 1,
                        new EmploymentPeriod(previous.start(), max(previous.end(), candidate.end())));
            } else {
                merged.add(candidate);
            }
        }
        int months = merged.stream()
                .mapToInt(period -> Math.toIntExact(ChronoUnit.MONTHS.between(period.start(), period.end()) + 1))
                .sum();
        return new ExperienceSummary(months, months / 12, months % 12);
    }

    public int commercialScore(int months) {
        if (months <= 0) return 0;
        if (months <= 5) return 2;
        if (months <= 11) return 4;
        if (months <= 17) return 7;
        if (months <= 23) return 8;
        if (months <= 35) return 9;
        return 10;
    }

    private static java.time.YearMonth max(java.time.YearMonth left, java.time.YearMonth right) {
        return left.isAfter(right) ? left : right;
    }
}
