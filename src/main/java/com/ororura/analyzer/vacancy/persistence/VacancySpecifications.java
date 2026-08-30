package com.ororura.analyzer.vacancy.persistence;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.ororura.analyzer.vacancy.VacancySearchCriteria;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import static com.ororura.analyzer.vacancy.persistence.VacancyNormalizer.searchText;

public final class VacancySpecifications {
    private VacancySpecifications() {
    }

    public static Specification<VacancyEntity> matching(VacancySearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            Expression<String> searchable = cb.lower(cb.concat(cb.concat(cb.coalesce(root.get("title"), ""), " "),
                    cb.concat(cb.coalesce(root.get("company"), ""), cb.concat(" ", cb.coalesce(root.get("description"), "")))));
            for (String token : searchText(criteria.query()).split(" ")) {
                if (!token.isBlank()) predicates.add(cb.like(searchable, "%" + escape(token) + "%", '\\'));
            }
            like(predicates, cb, root, "title", criteria.title());
            like(predicates, cb, root, "location", criteria.area());
            like(predicates, cb, root, "company", criteria.employer());
            if (criteria.workFormat() != null) {
                predicates.add(cb.equal(cb.upper(root.get("workFormat")), criteria.workFormat().name()));
            }
            if (Boolean.TRUE.equals(criteria.salaryOnly())) {
                predicates.add(cb.or(cb.isNotNull(root.get("salaryFrom")), cb.isNotNull(root.get("salaryTo"))));
            }
            if (criteria.currency() != null) {
                predicates.add(cb.equal(cb.upper(root.get("salaryCurrency")), criteria.currency().toUpperCase()));
            }
            if (criteria.salaryFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(cb.coalesce(root.get("salaryTo"), root.get("salaryFrom")),
                        criteria.salaryFrom()));
            }
            if (criteria.salaryTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(cb.coalesce(root.get("salaryFrom"), root.get("salaryTo")),
                        criteria.salaryTo()));
            }
            if (criteria.publishedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedAt"),
                        criteria.publishedFrom().atStartOfDay().atOffset(ZoneOffset.UTC)));
            }
            anyLike(predicates, cb, root, "experience", criteria.experience());
            anyLike(predicates, cb, root, "employment", criteria.employment());
            anyLike(predicates, cb, root, "schedule", criteria.schedule());
            if (criteria.level() != null) predicates.add(levelPredicate(root, cb, criteria.level()));
            for (String technology : criteria.technologies()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<VacancySkillEntity> skill = subquery.from(VacancySkillEntity.class);
                subquery.select(skill.get("vacancy").get("id")).where(
                        cb.equal(skill.get("vacancy").get("id"), root.get("id")),
                        cb.equal(skill.get("normalizedSkill"), searchText(technology)));
                predicates.add(cb.exists(subquery));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate levelPredicate(Root<VacancyEntity> root, jakarta.persistence.criteria.CriteriaBuilder cb,
            String level) {
        Expression<String> value = cb.lower(cb.coalesce(root.get("experience"), ""));
        return switch (searchText(level)) {
            case "junior" -> cb.or(cb.like(value, "%нет опыта%"), cb.like(value, "%1–3%"),
                    cb.like(value, "%1-3%"), cb.like(value, "%between1and3%"));
            case "middle" -> cb.or(cb.like(value, "%1–3%"), cb.like(value, "%3–6%"),
                    cb.like(value, "%between1and3%"), cb.like(value, "%between3and6%"));
            case "senior" -> cb.or(cb.like(value, "%3–6%"), cb.like(value, "%более 6%"),
                    cb.like(value, "%between3and6%"), cb.like(value, "%morethan6%"));
            default -> cb.like(value, "%" + escape(searchText(level)) + "%", '\\');
        };
    }

    private static void like(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<VacancyEntity> root, String field, String value) {
        if (value != null) predicates.add(cb.like(cb.lower(cb.coalesce(root.get(field), "")),
                "%" + escape(searchText(value)) + "%", '\\'));
    }

    private static void anyLike(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
            Root<VacancyEntity> root, String field, List<String> values) {
        if (values.isEmpty()) return;
        Expression<String> actual = cb.lower(cb.coalesce(root.get(field), ""));
        predicates.add(cb.or(values.stream().map(value -> cb.like(actual,
                "%" + escape(searchText(value)) + "%", '\\')).toArray(Predicate[]::new)));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
