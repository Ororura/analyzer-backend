package com.ororura.analyzer.vacancy.infrastructure.persistence;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

import com.ororura.analyzer.vacancy.domain.Vacancy;
import com.ororura.analyzer.vacancy.application.search.VacancySearchText;
import org.springframework.stereotype.Component;

@Component
public class VacancyNormalizer {

    public VacancyContent normalize(Vacancy vacancy) {
        var salary = vacancy.salary();
        List<VacancyContent.Skill> skills = vacancy.skills().stream()
                .map(VacancyNormalizer::clean)
                .filter(value -> !value.isEmpty())
                .map(value -> new VacancyContent.Skill(value, VacancySearchText.normalize(value)))
                .distinct()
                .sorted(java.util.Comparator.comparing(VacancyContent.Skill::normalized))
                .toList();
        return new VacancyContent(
                clean(vacancy.title()), nullable(vacancy.company()), nullable(vacancy.companyId()),
                nullable(vacancy.description()), skills, cleanList(vacancy.requirements()),
                cleanList(vacancy.responsibilities()), nullable(vacancy.experience()),
                nullable(vacancy.employment()), nullable(vacancy.schedule()), upper(vacancy.workFormat()),
                salary == null ? null : salary.from(), salary == null ? null : salary.to(),
                salary == null ? null : upper(salary.currency()), salary == null ? null : salary.gross(),
                nullable(vacancy.location()), nullable(vacancy.url()), date(vacancy.publishedAt()), null, null);
    }

    public Map<String, Object> rawPayload(Vacancy vacancy) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", vacancy.hhId());
        payload.put("title", vacancy.title());
        payload.put("company", vacancy.company());
        payload.put("companyId", vacancy.companyId());
        payload.put("url", vacancy.url());
        payload.put("location", vacancy.location());
        payload.put("salary", vacancy.salary());
        payload.put("description", vacancy.description());
        payload.put("skills", vacancy.skills());
        payload.put("requirements", vacancy.requirements());
        payload.put("responsibilities", vacancy.responsibilities());
        payload.put("experience", vacancy.experience());
        payload.put("employment", vacancy.employment());
        payload.put("schedule", vacancy.schedule());
        payload.put("workFormat", vacancy.workFormat());
        payload.put("publishedAt", vacancy.publishedAt());
        return payload;
    }

    private static List<String> cleanList(List<String> values) {
        return values.stream().map(VacancyNormalizer::clean).filter(value -> !value.isEmpty()).distinct().toList();
    }

    private static String clean(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace("\r\n", "\n").replace('\r', '\n').replaceAll("[\\t ]+", " ")
                .replaceAll(" *\\n *", "\n").trim();
    }

    private static String nullable(String value) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String upper(String value) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    private static OffsetDateTime date(String value) {
        try {
            return value == null ? null : OffsetDateTime.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
