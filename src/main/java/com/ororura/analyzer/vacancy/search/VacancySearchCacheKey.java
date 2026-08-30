package com.ororura.analyzer.vacancy.search;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.ororura.analyzer.vacancy.persistence.VacancyNormalizer;
import org.springframework.stereotype.Component;

@Component
public class VacancySearchCacheKey {
    public String create(long marketVersion, VacancySearchCriteria criteria) {
        String canonical = String.join("\u001f", text(criteria.query()), text(criteria.title()),
                text(criteria.area()), text(criteria.employer()), list(criteria.experience()), text(criteria.level()),
                value(criteria.workFormat()), value(criteria.salaryFrom()), value(criteria.salaryTo()),
                text(criteria.currency()), value(criteria.salaryOnly()), list(criteria.technologies()),
                value(criteria.publishedFrom()), value(criteria.sort()), Integer.toString(criteria.page()),
                Integer.toString(criteria.pageSize()), list(criteria.employment()), list(criteria.schedule()));
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
            return "v" + marketVersion + ":" + hash;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String list(java.util.List<String> values) {
        return values.stream().map(VacancySearchCacheKey::text).sorted().distinct()
                .collect(java.util.stream.Collectors.joining("\u001e"));
    }
    private static String text(String value) { return VacancyNormalizer.searchText(value); }
    private static String value(Object value) { return value == null ? "" : value.toString(); }
}
