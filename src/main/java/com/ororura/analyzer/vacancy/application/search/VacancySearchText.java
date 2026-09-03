package com.ororura.analyzer.vacancy.application.search;

import java.text.Normalizer;
import java.util.Locale;

public final class VacancySearchText {

    private VacancySearchText() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replaceAll("[^\\p{L}\\p{N}+#.]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
