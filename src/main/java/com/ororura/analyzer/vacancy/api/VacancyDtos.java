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
            return new MarketVacancy(id, title, skills, requirements, responsibilities, description, salary,
                    experience, employment, schedule, workFormat);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VacancySearchItem(
            String id,
            String hhId,
            String title,
            String company,
            Salary salary,
            String experience,
            String workFormat,
            String location,
            String publishedAt,
            String url,
            List<String> skills,
            List<String> requirements,
            String source) {
        public VacancySearchItem {
            skills = skills == null ? List.of() : List.copyOf(skills);
            requirements = requirements == null ? List.of() : List.copyOf(requirements);
        }

        public static VacancySearchItem from(Vacancy vacancy) {
            return new VacancySearchItem(vacancy.id(), vacancy.hhId(), vacancy.title(), vacancy.company(), vacancy.salary(),
                    vacancy.experience(), vacancy.workFormat(), vacancy.location(), vacancy.publishedAt(),
                    vacancy.url(), vacancy.skills(), vacancy.requirements(), vacancy.source());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Pagination(int page, int pageSize, Integer totalPages, boolean hasNext) {
    }

    public record VacancySearchResult(
            List<VacancySearchItem> items,
            int page,
            int pageSize,
            Integer totalPages,
            Long totalElements,
            Pagination pagination,
            List<String> warnings) {
        public VacancySearchResult {
            items = List.copyOf(items);
            warnings = List.copyOf(warnings);
        }
    }

    public record VacancyError(String code, String message) {
    }
}
