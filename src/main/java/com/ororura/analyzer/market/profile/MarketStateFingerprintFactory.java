package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ororura.analyzer.market.requirement.MarketRequirementStatistics;
import org.springframework.stereotype.Component;

@Component
public class MarketStateFingerprintFactory {

    /** Frequencies are rounded to percentage points so sub-1% sampling noise does not trigger criterion synthesis. */
    public String create(MarketRequirementStatistics statistics) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, statistics.profile().name());
            statistics.requirements().stream().sorted(java.util.Comparator.comparing(value -> value.id()))
                    .forEach(value -> {
                        update(digest, value.id());
                        update(digest, value.type().name());
                        update(digest, bucket(value.frequency()));
                        update(digest, bucket(value.requiredFrequency()));
                        update(digest, bucket(value.preferredFrequency()));
                        update(digest, bucket(value.optionalFrequency()));
                    });
            return "sha256:" + java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String bucket(double frequency) {
        return Long.toString(Math.round(frequency * 100));
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
