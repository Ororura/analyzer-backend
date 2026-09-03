package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
class CriterionIdFactory {

    String create(String label, List<String> requirementIds) {
        String slug = Normalizer.normalize(label.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isEmpty()) {
            throw new MarketCriterionGenerationException("Criterion label cannot produce a stable id");
        }
        MessageDigest digest = sha256();
        requirementIds.stream().sorted(Comparator.naturalOrder()).forEach(value -> update(digest, value));
        String fingerprint = HexFormat.of().formatHex(digest.digest()).substring(0, 8);
        return slug + "-" + fingerprint;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
