package com.ororura.analyzer.resume.market;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MarketRequirementId {

    private static final Pattern SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private MarketRequirementId() {
    }

    public static String of(RequirementType type, String canonicalLabel) {
        if (type == null || canonicalLabel == null || canonicalLabel.isBlank()) {
            throw new IllegalArgumentException("Requirement type and canonical label must be defined");
        }
        String normalized = Normalizer.normalize(canonicalLabel.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        String slug = SEPARATOR.matcher(normalized).replaceAll("-").replaceAll("^-|-$", "");
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Canonical requirement label must contain letters or digits");
        }
        return type.name().toLowerCase(Locale.ROOT) + ":" + slug;
    }
}
