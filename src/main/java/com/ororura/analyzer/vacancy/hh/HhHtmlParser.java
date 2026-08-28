package com.ororura.analyzer.vacancy.hh;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ororura.analyzer.vacancy.api.VacancyDtos.Salary;
import com.ororura.analyzer.vacancy.hh.dto.HhDtos.SearchPage;
import com.ororura.analyzer.vacancy.hh.dto.HhDtos.SourceVacancy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public final class HhHtmlParser {

    private static final int ANTI_BOT_SCAN_LIMIT = 200_000;

    private final ObjectMapper objectMapper;

    public HhHtmlParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    SearchPage parseSearch(String html, int page, int perPage) {
        JsonNode result = initialState(html).path("vacancySearchResult");
        JsonNode vacancies = result.path("vacancies");
        if (!vacancies.isArray()) {
            throw HhExceptionMapper.error("UPSTREAM", "Не удалось распознать ответ HH.ru", 502, null, null);
        }
        List<SourceVacancy> items = new ArrayList<>();
        vacancies.forEach(node -> items.add(searchVacancy(node)));

        JsonNode lastPage = result.path("paging").path("lastPage").path("page");
        Integer totalPages = lastPage.canConvertToInt()
                ? lastPage.asInt() + 1
                : result.path("totalResults").canConvertToInt()
                        ? (int) Math.ceil((double) result.path("totalResults").asInt() / perPage)
                        : null;
        JsonNode next = result.path("paging").path("next");
        boolean hasNext = next.isObject()
                ? !next.path("disabled").asBoolean(false)
                : totalPages != null && page + 1 < totalPages;
        return new SearchPage(items, totalPages, hasNext);
    }

    SourceVacancy parseDetail(String html, String expectedId) {
        JsonNode node = initialState(html).path("vacancyView");
        if (!node.isObject()) {
            throw HhExceptionMapper.error(
                    "UPSTREAM", "Не удалось распознать страницу вакансии HH.ru", 502, null, null);
        }
        String id = valueOr(text(node, "vacancyId"), expectedId);
        return new SourceVacancy(
                id,
                text(node, "name"),
                companyName(node),
                companyId(node),
                "https://hh.ru/vacancy/" + id,
                location(node),
                salary(node),
                HhTextCleaner.clean(text(node, "description")),
                keySkills(node.path("keySkills").path("keySkill")),
                List.of(),
                List.of(),
                displayExperience(text(node, "workExperience")),
                displayEmployment(text(node, "employmentForm")),
                null,
                displayWorkFormats(node.path("workFormats")),
                text(node, "publicationDate"));
    }

    private JsonNode initialState(String html) {
        if (html == null) {
            throw HhExceptionMapper.error("UPSTREAM", "HH.ru вернул пустой ответ", 502, null, null);
        }
        Document document = Jsoup.parse(html);
        String sample = html.substring(0, Math.min(html.length(), ANTI_BOT_SCAN_LIMIT))
                .toLowerCase(Locale.ROOT);
        if (sample.contains("проверка, что вы не робот")
                || sample.contains("robot-check")
                || !document.select("input[name*=captcha], form[action*=captcha], [data-qa*=captcha]").isEmpty()) {
            throw HhExceptionMapper.error("FORBIDDEN", "HH.ru отклонил запрос", 403, null, null);
        }
        Element state = document.getElementById("HH-Lux-InitialState");
        if (state == null) {
            throw HhExceptionMapper.error("UPSTREAM", "Не удалось распознать ответ HH.ru", 502, null, null);
        }
        try {
            return objectMapper.readTree(state.wholeText());
        } catch (Exception exception) {
            throw HhExceptionMapper.error(
                    "UPSTREAM", "Не удалось распознать ответ HH.ru", 502, null, exception);
        }
    }

    private static SourceVacancy searchVacancy(JsonNode node) {
        JsonNode snippet = node.path("snippet");
        String id = text(node, "vacancyId");
        String requirement = text(snippet, "req");
        String responsibility = text(snippet, "resp");
        return new SourceVacancy(
                id,
                text(node, "name"),
                companyName(node),
                companyId(node),
                firstNonBlank(text(node.path("links"), "desktop"), "https://hh.ru/vacancy/" + id),
                location(node),
                salary(node),
                firstNonBlank(text(snippet, "desc"), requirement),
                commaSeparated(text(snippet, "skill")),
                listOf(requirement),
                listOf(responsibility),
                displayExperience(text(node, "workExperience")),
                displayEmployment(firstNonBlank(
                        text(node, "employmentForm"), text(node.path("employment"), "@type"))),
                displaySchedule(text(node, "@workSchedule")),
                displayWorkFormats(node.path("workFormats")),
                text(node.path("publicationTime"), "$"));
    }

    private static String companyName(JsonNode node) {
        return firstNonBlank(text(node.path("company"), "visibleName"), text(node.path("company"), "name"));
    }

    private static String companyId(JsonNode node) {
        return text(node.path("company"), "id");
    }

    private static String location(JsonNode node) {
        return firstNonBlank(text(node.path("address"), "displayName"), text(node.path("area"), "name"));
    }

    private static Salary salary(JsonNode node) {
        JsonNode value = node.path("compensation");
        if (!value.isObject() || value.size() == 0) return null;
        return new Salary(integer(value, "from"), integer(value, "to"), text(value, "currencyCode"), bool(value, "gross"));
    }

    private static List<String> keySkills(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        node.forEach(value -> {
            if (value.isString() && !value.stringValue().isBlank()) result.add(value.stringValue().trim());
        });
        return List.copyOf(result);
    }

    private static List<String> commaSeparated(String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    private static List<String> listOf(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.trim());
    }

    private static String displayExperience(String value) {
        if (value == null) return null;
        return switch (value) {
            case "noExperience" -> "Нет опыта";
            case "between1And3" -> "1–3 года";
            case "between3And6" -> "3–6 лет";
            case "moreThan6" -> "Более 6 лет";
            default -> value;
        };
    }

    private static String displayEmployment(String value) {
        if (value == null) return null;
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "FULL" -> "Полная занятость";
            case "PART" -> "Частичная занятость";
            case "PROJECT" -> "Проектная работа";
            case "PROBATION" -> "Стажировка";
            default -> value;
        };
    }

    private static String displaySchedule(String value) {
        if (value == null) return null;
        return switch (value) {
            case "fullDay" -> "Полный день";
            case "shift" -> "Сменный график";
            case "flexible" -> "Гибкий график";
            case "remote" -> "Удалённая работа";
            case "flyInFlyOut" -> "Вахтовый метод";
            default -> value;
        };
    }

    private static String displayWorkFormats(JsonNode node) {
        List<String> values = new ArrayList<>();
        collectText(node, values);
        return values.stream()
                .map(value -> switch (value) {
                    case "REMOTE" -> "Удалённо";
                    case "HYBRID" -> "Гибрид";
                    case "ON_SITE" -> "На месте";
                    default -> value;
                })
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    private static void collectText(JsonNode node, List<String> result) {
        if (node.isString()) {
            result.add(node.stringValue());
        } else if (node.isArray() || node.isObject()) {
            node.forEach(child -> collectText(child, result));
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isValueNode() && !value.isNull() ? value.asString() : null;
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToInt() ? value.asInt() : null;
    }

    private static Boolean bool(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isBoolean() ? value.asBoolean() : null;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String valueOr(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
