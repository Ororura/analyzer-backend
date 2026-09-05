package com.ororura.analyzer.analysis.domain;

import java.time.Instant;
import java.util.UUID;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;

public record AnalysisRun(UUID id, UUID profileId, long profileVersion, Status status,
        Instant createdAt, Instant completedAt, String failureCode, CandidateGrade targetGrade,
        CandidateGrade detectedGrade, EffectiveAnalysisConfig effectiveConfig, ResumeAnalysisResult result) {
    public enum Status { PREPARING, RUNNING, COMPLETED, FAILED }
}
