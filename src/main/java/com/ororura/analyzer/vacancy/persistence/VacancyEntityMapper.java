package com.ororura.analyzer.vacancy.persistence;

import java.time.Instant;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Salary;
import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import org.springframework.stereotype.Component;

@Component
public class VacancyEntityMapper {

    public Vacancy toDomain(VacancyEntity entity) {
        Salary salary = entity.salaryFrom() == null && entity.salaryTo() == null ? null
                : new Salary(entity.salaryFrom(), entity.salaryTo(), entity.salaryCurrency(), entity.salaryGross());
        String stableId = sourcePrefix(entity.source()) + entity.externalId();
        return new Vacancy(stableId, entity.externalId(), entity.title(), entity.company(), entity.companyId(),
                entity.url(), entity.location(), salary, entity.description(),
                entity.skills().stream().map(VacancySkillEntity::skill).toList(), entity.requirements(),
                entity.responsibilities(), entity.experience(), entity.employment(), entity.schedule(),
                entity.workFormat(), entity.source(), entity.publishedAt() == null ? null : entity.publishedAt().toString(),
                entity.lastCheckedAt() == null ? Instant.now().toString() : entity.lastCheckedAt().toInstant().toString());
    }

    public static String sourcePrefix(String source) {
        return "hh.ru".equalsIgnoreCase(source) || "hh".equalsIgnoreCase(source) ? "hh-" : source + "-";
    }
}
