package com.ororura.analyzer.vacancy.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;

public record VacancyContent(
        String title,
        String company,
        String companyId,
        String description,
        List<Skill> skills,
        List<String> requirements,
        List<String> responsibilities,
        String experience,
        String employment,
        String schedule,
        String workFormat,
        Integer salaryFrom,
        Integer salaryTo,
        String salaryCurrency,
        Boolean salaryGross,
        String location,
        String url,
        OffsetDateTime publishedAt,
        OffsetDateTime sourceCreatedAt,
        OffsetDateTime sourceUpdatedAt) {

    public record Skill(String value, String normalized) {
    }
}
