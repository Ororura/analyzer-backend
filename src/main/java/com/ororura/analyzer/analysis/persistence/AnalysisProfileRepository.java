package com.ororura.analyzer.analysis.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

public interface AnalysisProfileRepository extends JpaRepository<AnalysisProfileEntity, UUID> {
    Page<AnalysisProfileEntity> findByDeletedAtIsNull(Pageable pageable);
    Optional<AnalysisProfileEntity> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);
    Page<AnalysisProfileEntity> findByOwnerIdAndDeletedAtIsNull(UUID ownerId, Pageable pageable);
}
