package com.ororura.analyzer.analysis.persistence;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRunEntity, UUID> {
    Optional<AnalysisRunEntity> findByIdAndOwnerId(UUID id,UUID ownerId);
}
