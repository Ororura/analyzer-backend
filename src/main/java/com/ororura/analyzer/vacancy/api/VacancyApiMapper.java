package com.ororura.analyzer.vacancy.api;

import com.ororura.analyzer.vacancy.application.port.VacancyQueryResult;
import com.ororura.analyzer.vacancy.domain.Salary;
import com.ororura.analyzer.vacancy.domain.Vacancy;
import org.springframework.stereotype.Component;

@Component
public class VacancyApiMapper {

    public VacancySearchResult toSearchResponse(VacancyQueryResult result) {
        var items = result.items().stream().map(this::toSearchItem).toList();
        var pagination = new Pagination(result.page(), result.pageSize(), result.totalPages(), result.hasNext());
        return new VacancySearchResult(items, result.page(), result.pageSize(), result.totalPages(),
                result.totalElements(), pagination, result.warnings());
    }

    public VacancyResponse toResponse(Vacancy vacancy) {
        return new VacancyResponse(vacancy.id(), vacancy.hhId(), vacancy.title(), vacancy.company(),
                vacancy.companyId(), vacancy.url(), vacancy.location(), salary(vacancy.salary()),
                vacancy.description(), vacancy.skills(), vacancy.requirements(), vacancy.responsibilities(),
                vacancy.experience(), vacancy.employment(), vacancy.schedule(), vacancy.workFormat(),
                vacancy.source(), vacancy.publishedAt(), vacancy.normalizedAt());
    }

    private VacancySearchItem toSearchItem(Vacancy vacancy) {
        return new VacancySearchItem(vacancy.id(), vacancy.hhId(), vacancy.title(), vacancy.company(),
                salary(vacancy.salary()), vacancy.experience(), vacancy.workFormat(), vacancy.location(),
                vacancy.publishedAt(), vacancy.url(), vacancy.skills(), vacancy.requirements(), vacancy.source());
    }

    private static SalaryResponse salary(Salary salary) {
        return salary == null ? null : new SalaryResponse(salary.from(), salary.to(), salary.currency(), salary.gross());
    }
}
