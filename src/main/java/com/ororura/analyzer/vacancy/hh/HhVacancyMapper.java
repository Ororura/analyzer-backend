package com.ororura.analyzer.vacancy.hh;

import java.time.Instant;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import com.ororura.analyzer.vacancy.hh.dto.HhDtos.SourceVacancy;
import org.springframework.stereotype.Component;

@Component
public final class HhVacancyMapper {

    Vacancy toDomain(SourceVacancy source) {
        return new Vacancy(
                "hh-" + source.id(),
                source.id(),
                valueOr(source.title(), ""),
                valueOr(source.company(), "Неизвестно"),
                valueOr(source.companyId(), ""),
                valueOr(source.url(), "https://hh.ru/vacancy/" + source.id()),
                source.location(),
                source.salary(),
                valueOr(source.description(), ""),
                source.skills(),
                source.requirements(),
                source.responsibilities(),
                source.experience(),
                source.employment(),
                source.schedule(),
                source.workFormat(),
                "hh.ru",
                source.publishedAt(),
                Instant.now().toString());
    }

    SourceVacancy merge(SourceVacancy search, SourceVacancy detail) {
        return new SourceVacancy(
                valueOr(detail.id(), search.id()),
                valueOr(detail.title(), search.title()),
                valueOr(detail.company(), search.company()),
                valueOr(detail.companyId(), search.companyId()),
                valueOr(detail.url(), search.url()),
                valueOr(detail.location(), search.location()),
                detail.salary() == null ? search.salary() : detail.salary(),
                valueOr(detail.description(), search.description()),
                detail.skills().isEmpty() ? search.skills() : detail.skills(),
                detail.requirements().isEmpty() ? search.requirements() : detail.requirements(),
                detail.responsibilities().isEmpty() ? search.responsibilities() : detail.responsibilities(),
                valueOr(detail.experience(), search.experience()),
                valueOr(detail.employment(), search.employment()),
                valueOr(detail.schedule(), search.schedule()),
                valueOr(detail.workFormat(), search.workFormat()),
                valueOr(detail.publishedAt(), search.publishedAt()));
    }

    private static String valueOr(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
