package com.ororura.analyzer.resume.domain;

import java.time.YearMonth;

public final class ExperienceModels {

    private ExperienceModels() {
    }

    public record EmploymentPeriod(YearMonth start, YearMonth end) {
        public EmploymentPeriod {
            if (start == null || end == null) {
                throw new IllegalArgumentException("Employment period boundaries are required");
            }
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("Employment period end precedes start");
            }
        }
    }

    public record ExperienceSummary(int commercialMonths, int commercialYears, int remainingMonths) {
    }
}
