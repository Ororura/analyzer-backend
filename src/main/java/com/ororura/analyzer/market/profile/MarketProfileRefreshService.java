package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketProfileRefreshService {
    private static final Logger log = LoggerFactory.getLogger(MarketProfileRefreshService.class);
    private final MarketStateFingerprintFactory fingerprints;
    private final MarketAnalysisProfileGenerationService generation;
    private final ConcurrentMap<ResumeAnalysisProfile, String> publishedMarketStates = new ConcurrentHashMap<>();

    public MarketProfileRefreshService(MarketStateFingerprintFactory fingerprints,
            MarketAnalysisProfileGenerationService generation) {
        this.fingerprints = fingerprints;
        this.generation = generation;
    }

    public RefreshResult refresh(MarketRequirementStatistics statistics) {
        log.info("Market profile refresh evaluated profile={} fetched={} processed={} failed={} requirementsAfterFiltering={}",
                statistics.profile(), statistics.fetchedCount(), statistics.processedCount(), statistics.failedCount(),
                statistics.requirements().size());
        if (!statistics.sufficientSample() || statistics.requirements().isEmpty()) {
            log.info("Market profile reused profile={} reason=insufficient_sample", statistics.profile());
            return new RefreshResult(Status.REUSED_INSUFFICIENT_SAMPLE, null, null);
        }
        String fingerprint = fingerprints.create(statistics);
        if (fingerprint.equals(publishedMarketStates.get(statistics.profile()))) {
            log.info("Market profile reused profile={} marketFingerprint={}", statistics.profile(), fingerprint);
            return new RefreshResult(Status.REUSED_UNCHANGED, fingerprint, null);
        }
        MarketAnalysisProfile profile = generation.generateAndPublish(statistics);
        publishedMarketStates.put(statistics.profile(), fingerprint);
        log.info("Market profile regenerated profile={} criteriaCount={} profileVersion={} marketFingerprint={} source=LIVE",
                profile.profile(), profile.criteria().size(), profile.version(), fingerprint);
        return new RefreshResult(Status.REGENERATED, fingerprint, profile);
    }

    public enum Status { REGENERATED, REUSED_UNCHANGED, REUSED_INSUFFICIENT_SAMPLE }
    public record RefreshResult(Status status, String marketFingerprint, MarketAnalysisProfile profile) {}
}
