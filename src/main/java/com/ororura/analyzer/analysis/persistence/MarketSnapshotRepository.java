package com.ororura.analyzer.analysis.persistence;

import java.util.*;
import org.springframework.data.repository.Repository;

public interface MarketSnapshotRepository extends Repository<MarketSnapshotEntity, UUID> {
    MarketSnapshotEntity save(MarketSnapshotEntity value);
    Optional<MarketSnapshotEntity> findById(UUID id);
}
