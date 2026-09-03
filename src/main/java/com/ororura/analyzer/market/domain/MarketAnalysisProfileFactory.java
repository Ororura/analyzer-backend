package com.ororura.analyzer.market.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;

public class MarketAnalysisProfileFactory {

    public MarketAnalysisProfile create(ResumeAnalysisProfile profile, String targetRole,
            List<AnalysisCriterion> criteria, List<MarketRequirement> requirements,
            int sampleSize, Instant generatedAt) {
        String version = fingerprint(profile, targetRole, criteria, requirements, sampleSize);
        return new MarketAnalysisProfile(profile, targetRole, criteria, requirements,
                sampleSize, generatedAt, version);
    }

    public MarketAnalysisProfile create(ResumeAnalysisProfile profile, String targetRole,
            List<AnalysisCriterion> criteria, List<MarketRequirement> requirements,
            List<MarketSkillStatistics> skillStatistics, List<MarketVacancyRequirements> vacancyRequirements,
            boolean sufficientSample, int sampleSize, Instant generatedAt) {
        String version = fingerprint(profile, targetRole, criteria, requirements, sampleSize);
        return new MarketAnalysisProfile(profile, targetRole, criteria, requirements, skillStatistics,
                vacancyRequirements, sufficientSample, sampleSize, generatedAt, version);
    }

    String fingerprint(ResumeAnalysisProfile profile, String targetRole,
            List<AnalysisCriterion> criteria, List<MarketRequirement> requirements, int sampleSize) {
        MessageDigest digest = sha256();
        update(digest, profile == null ? null : profile.name());
        update(digest, targetRole);
        update(digest, Integer.toString(sampleSize));
        if (criteria != null) {
            update(digest, Integer.toString(criteria.size()));
            for (AnalysisCriterion criterion : criteria) {
                update(digest, criterion.id());
                update(digest, criterion.label());
                update(digest, criterion.description());
                update(digest, Double.toHexString(criterion.weight()));
                update(digest, Double.toHexString(criterion.marketFrequency()));
            }
        } else {
            update(digest, null);
        }
        if (requirements != null) {
            update(digest, Integer.toString(requirements.size()));
            for (MarketRequirement requirement : requirements) {
                update(digest, requirement.id());
                update(digest, requirement.label());
                update(digest, requirement.type().name());
                update(digest, Double.toHexString(requirement.frequency()));
            }
        } else {
            update(digest, null);
        }
        return "sha256:" + HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
