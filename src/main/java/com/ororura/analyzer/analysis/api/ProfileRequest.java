package com.ororura.analyzer.analysis.api;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;

/** Null settings inherit preset defaults on creation only. PUT requires a complete profile. */
public record ProfileRequest(@NotBlank @Size(max=120) String name, ResumeAnalysisProfile preset,
        CareerDirection direction, @Size(max=80) String specialization, CandidateGrade targetGrade,
        @Size(max=30) List<@NotBlank @Size(max=80) String> technologies, @Valid MarketFilters marketFilters) {}
