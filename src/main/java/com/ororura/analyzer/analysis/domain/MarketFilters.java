package com.ororura.analyzer.analysis.domain;

import java.time.LocalDate;
import java.util.List;
import com.ororura.analyzer.vacancy.search.WorkFormat;
import jakarta.validation.constraints.*;

public record MarketFilters(
        @Size(max=200) String location, @Size(max=200) String employer,
        @Size(max=10) List<@NotBlank @Size(max=100) String> employment,
        @Size(max=10) List<@NotBlank @Size(max=100) String> schedule,
        WorkFormat workFormat, @PositiveOrZero Integer salaryFrom, @PositiveOrZero Integer salaryTo,
        @Size(max=10) String currency, Boolean salaryOnly, LocalDate publishedFrom,
        CandidateGrade searchGrade, Boolean includeUnknownGrade) {
    public MarketFilters {
        includeUnknownGrade = Boolean.TRUE.equals(includeUnknownGrade);
        employment = employment == null ? List.of() : List.copyOf(employment);
        schedule = schedule == null ? List.of() : List.copyOf(schedule);
        if (salaryFrom != null && salaryTo != null && salaryFrom > salaryTo)
            throw new IllegalArgumentException("salaryFrom must not exceed salaryTo");
    }
    public static MarketFilters defaults() {
        return new MarketFilters(null,null,List.of(),List.of(),null,null,null,null,null,null,null,false);
    }
}
