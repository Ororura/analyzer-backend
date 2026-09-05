package com.ororura.analyzer.analysis.domain;

import java.time.Instant;
import java.util.*;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

public record UserAnalysisProfile(UUID id, long version, String name, CareerDirection direction,
        String specialization, CandidateGrade targetGrade, List<String> technologies, MarketFilters marketFilters,
        ResumeAnalysisProfile preset, String scoringPolicyVersion, Instant createdAt, Instant updatedAt) {
    public UserAnalysisProfile { technologies = List.copyOf(technologies); }
    public MarketSegment segment() {
        return new MarketSegment(direction, specialization, targetGrade, technologies, marketFilters,
                marketFilters.searchGrade() == null ? targetGrade : marketFilters.searchGrade());
    }
}
