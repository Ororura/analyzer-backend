package com.ororura.analyzer.vacancy.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ororura.analyzer.vacancy.market.MarketVacancy;

public final class VacancyDtos {

    private VacancyDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Salary(Integer from, Integer to, String currency, Boolean gross) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Vacancy(
            String id,
            String hhId,
            String title,
            String company,
            String companyId,
            String url,
            String location,
            Salary salary,
            String description,
            List<String> skills,
            List<String> requirements,
            List<String> responsibilities,
            String experience,
            String employment,
            String schedule,
            String workFormat,
            String source,
            String publishedAt,
            String normalizedAt) {

        public Vacancy {
            skills = List.copyOf(skills);
            requirements = List.copyOf(requirements);
            responsibilities = List.copyOf(responsibilities);
        }

        public MarketVacancy toMarketVacancy() {
            return new MarketVacancy(skills, experience, employment, schedule, workFormat);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Pagination(int page, int pageSize, Integer totalPages, boolean hasNext) {
    }

    public record VacancySearchResult(List<Vacancy> items, Pagination pagination, List<String> warnings) {
        public VacancySearchResult {
            items = List.copyOf(items);
            warnings = List.copyOf(warnings);
        }
    }

    public record VacancyError(String code, String message) {
    }
}
