package com.ororura.analyzer.resume.api;

import java.util.List;
import java.util.Map;
import com.ororura.analyzer.resume.api.SkillRoiAnalysis.Effort;

public record ResumeRecommendationAnalysis(List<Recommendation> items) {
    public ResumeRecommendationAnalysis { items = List.copyOf(items); }
    public record Recommendation(String title, Category category, Priority priority, Effort effort,
            Map<String, Integer> expectedImpact, String reason) {
        public Recommendation { expectedImpact = Map.copyOf(expectedImpact); }
    }
    public enum Category { ATS, SKILL, EXPERIENCE, CLAIM, VACANCY }
    public enum Priority { CRITICAL, HIGH, MEDIUM, LOW }
}
