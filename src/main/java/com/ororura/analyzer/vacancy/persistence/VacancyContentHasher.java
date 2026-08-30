package com.ororura.analyzer.vacancy.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class VacancyContentHasher {

    public String hash(VacancyContent content) {
        StringBuilder canonical = new StringBuilder();
        add(canonical, content.title());
        add(canonical, content.company());
        add(canonical, content.companyId());
        add(canonical, content.description());
        add(canonical, content.experience());
        add(canonical, content.employment());
        add(canonical, content.schedule());
        add(canonical, content.workFormat());
        add(canonical, content.salaryFrom());
        add(canonical, content.salaryTo());
        add(canonical, content.salaryCurrency());
        add(canonical, content.salaryGross());
        add(canonical, content.location());
        add(canonical, content.url());
        addList(canonical, content.skills().stream().map(VacancyContent.Skill::normalized).sorted().toList());
        addList(canonical, content.requirements());
        addList(canonical, content.responsibilities());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void addList(StringBuilder target, List<String> values) {
        add(target, values.size());
        values.forEach(value -> add(target, value));
    }

    private static void add(StringBuilder target, Object value) {
        String text = value == null ? "" : value.toString();
        target.append(text.length()).append(':').append(text).append('|');
    }
}
