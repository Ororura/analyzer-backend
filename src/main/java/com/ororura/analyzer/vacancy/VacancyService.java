package com.ororura.analyzer.vacancy;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Vacancy;
import com.ororura.analyzer.vacancy.api.VacancyDtos.VacancySearchItem;
import org.springframework.stereotype.Service;

import static com.ororura.analyzer.vacancy.api.VacancyDtos.*;

@Service
public class VacancyService {

    private static final int MAX_PAGES = 10;
    private static final int MAX_VACANCIES = 200;
    private static final int MAX_PER_PAGE = 50;

    private final VacancyProvider provider;

    public VacancyService(VacancyProvider provider) {
        this.provider = provider;
    }

    public VacancySearchResult search(VacancySearchQuery request) {
        return search(request.toCriteria());
    }

    public VacancySearchResult search(VacancySearchCriteria request) {
        DomainSearchResult source = searchDomain(request);
        VacancySearchCriteria query = source.criteria();
        int maxPagesForSize = Math.min(MAX_PAGES, (int) Math.ceil((double) MAX_VACANCIES / query.pageSize()));
        Integer totalPages = source.totalPages() == null
                ? null
                : Math.min(source.totalPages(), maxPagesForSize);
        boolean hasNext = source.hasNext() && query.page() + 1 < maxPagesForSize;
        return new VacancySearchResult(
                source.items().stream().map(VacancySearchItem::from).toList(),
                query.page(), query.pageSize(), totalPages, source.totalElements(),
                new Pagination(query.page(), query.pageSize(), totalPages, hasNext),
                source.warnings());
    }

    public DomainSearchResult searchDomain(VacancySearchCriteria request) {
        VacancySearchCriteria query = normalize(request);
        VacancyProviderSearchResult source = provider.search(query);
        List<Vacancy> filtered = source.items().stream().filter(vacancy -> matches(vacancy, query)).toList();
        return new DomainSearchResult(filtered, source.totalPages(), source.totalElements(),
                source.hasNext(), source.warnings(), query);
    }

    public Vacancy getById(String vacancyId) {
        return provider.getById(vacancyId);
    }

    public List<Vacancy> getByIds(List<String> vacancyIds) {
        return provider.getByIds(vacancyIds);
    }

    private VacancySearchCriteria normalize(VacancySearchCriteria query) {
        if (query == null) throw new InvalidVacancyQueryException("criteria обязателен");
        if (query.page() < 0 || query.page() >= MAX_PAGES) {
            throw new InvalidVacancyQueryException("page должен быть от 0 до 9");
        }
        if (query.pageSize() < 1 || query.pageSize() > MAX_PER_PAGE) {
            throw new InvalidVacancyQueryException("pageSize должен быть от 1 до 50");
        }
        if (query.page() * query.pageSize() >= MAX_VACANCIES) {
            throw new InvalidVacancyQueryException("Запрос превышает maxVacancies=200");
        }
        String text = query.query() == null || query.query().trim().isEmpty()
                ? "Java Backend Developer"
                : query.query().trim();
        if (query.title() != null && !query.title().isBlank()) text = text + " " + query.title().trim();
        if (!query.technologies().isEmpty()) text = text + " " + String.join(" ", query.technologies());
        if (text.length() > 200) {
            throw new InvalidVacancyQueryException("Поисковый запрос слишком длинный");
        }
        if (query.salaryFrom() != null && query.salaryTo() != null && query.salaryFrom() > query.salaryTo()) {
            throw new InvalidVacancyQueryException("salaryFrom не может быть больше salaryTo");
        }
        String area = query.area() == null || query.area().trim().isEmpty()
                ? null
                : query.area().trim();
        return new VacancySearchCriteria(text, blankToNull(query.title()), area, blankToNull(query.employer()),
                query.experience(), blankToNull(query.level()), query.workFormat(), query.salaryFrom(), query.salaryTo(),
                blankToNull(query.currency()), query.salaryOnly(), query.technologies(), query.publishedFrom(), query.sort(),
                query.page(), query.pageSize(), query.employment(), query.schedule());
    }

    private static boolean matches(Vacancy vacancy, VacancySearchCriteria criteria) {
        if (!contains(vacancy.title(), criteria.title()) || !contains(vacancy.company(), criteria.employer())) return false;
        if (Boolean.TRUE.equals(criteria.salaryOnly()) && vacancy.salary() == null) return false;
        if (criteria.currency() != null && (vacancy.salary() == null
                || !criteria.currency().equalsIgnoreCase(vacancy.salary().currency()))) return false;
        if (criteria.salaryFrom() != null && (vacancy.salary() == null || vacancy.salary().to() != null
                && vacancy.salary().to() < criteria.salaryFrom()
                || vacancy.salary() != null && vacancy.salary().to() == null && vacancy.salary().from() != null
                && vacancy.salary().from() < criteria.salaryFrom())) return false;
        if (criteria.salaryTo() != null && vacancy.salary() != null && vacancy.salary().from() != null
                && vacancy.salary().from() > criteria.salaryTo()) return false;
        if (criteria.workFormat() != null && !workFormat(vacancy).equals(criteria.workFormat())) return false;
        if (criteria.publishedFrom() != null && publishedDate(vacancy) != null
                && publishedDate(vacancy).toLocalDate().isBefore(criteria.publishedFrom())) return false;
        if (criteria.level() != null && !matchesLevel(vacancy.experience(), criteria.level())) return false;
        return criteria.technologies().stream().allMatch(technology -> vacancyText(vacancy).contains(normalize(technology)));
    }

    private static String vacancyText(Vacancy vacancy) {
        return normalize(String.join(" ", vacancy.title(), vacancy.description(), String.join(" ", vacancy.skills()),
                String.join(" ", vacancy.requirements()), String.join(" ", vacancy.responsibilities())));
    }

    private static WorkFormat workFormat(Vacancy vacancy) {
        String value = normalize((vacancy.workFormat() == null ? "" : vacancy.workFormat()) + " "
                + (vacancy.schedule() == null ? "" : vacancy.schedule()));
        if (value.contains("hybrid") || value.contains("гибрид")) return WorkFormat.HYBRID;
        if (value.contains("remote") || value.contains("удален") || value.contains("удалён")) return WorkFormat.REMOTE;
        return WorkFormat.OFFICE;
    }

    private static boolean matchesLevel(String experience, String level) {
        String actual = normalize(experience);
        return switch (normalize(level)) {
            case "junior" -> actual.contains("нет опыта") || actual.contains("1 3") || actual.contains("noexperience")
                    || actual.contains("between1and3");
            case "middle" -> actual.contains("1 3") || actual.contains("3 6") || actual.contains("between1and3")
                    || actual.contains("between3and6");
            case "senior" -> actual.contains("3 6") || actual.contains("более 6") || actual.contains("between3and6")
                    || actual.contains("morethan6");
            default -> actual.contains(normalize(level));
        };
    }

    private static OffsetDateTime publishedDate(Vacancy vacancy) {
        try { return vacancy.publishedAt() == null ? null : OffsetDateTime.parse(vacancy.publishedAt()); }
        catch (RuntimeException ignored) { return null; }
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('-', ' ').replaceAll("[^\\p{L}\\p{N}+#.]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static boolean contains(String actual, String expected) {
        return expected == null || normalize(actual).contains(normalize(expected));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DomainSearchResult(List<Vacancy> items, Integer totalPages, Long totalElements,
            boolean hasNext, List<String> warnings, VacancySearchCriteria criteria) {
    }
}
