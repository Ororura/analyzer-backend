package com.ororura.analyzer.resume.domain;

import com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy;

import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.Evidence;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.domain.MarketRequirement;
import com.ororura.analyzer.resume.domain.analysis.AtsAnalysis;
import com.ororura.analyzer.resume.domain.analysis.ScoreBreakdown;
import com.ororura.analyzer.analysis.semantic.LlmResumeAnalysisResponse;
import java.util.List;
public class AtsScoreCalculator {
    private final AnalysisScoringPolicy scoring;

    public AtsScoreCalculator(AnalysisScoringPolicy scoring) { this.scoring = scoring; }

    public AtsScore calculate(int atsReadability, int resumeQuality, int experienceDescription,
            int commercialExperience, TechnologyProfile technologies, MarketAnalysisProfile profile) {
        int coverage = weighted(technologies, profile, requirement -> 1.0);
        int relevance = weighted(technologies, profile, MarketRequirement::frequency);
        int total = ScoreMath.score(atsReadability * 10 * .25 + coverage * .25 + relevance * .20
                + resumeQuality * 10 * .15 + experienceDescription * 10 * .10
                + commercialExperience * 10 * .05);
        return new AtsScore(total, coverage, relevance);
    }

    private int weighted(TechnologyProfile technologies, MarketAnalysisProfile profile,
            java.util.function.ToDoubleFunction<MarketRequirement> weight) {
        double earned = 0;
        double available = 0;
        for (MarketRequirement requirement : profile.requirements()) {
            double currentWeight = weight.applyAsDouble(requirement);
            if (currentWeight <= 0) continue;
            available += currentWeight;
            earned += currentWeight * evidenceWeight(technologies.evidence().get(requirement.label()));
        }
        return available == 0 ? 0 : ScoreMath.score(earned / available * 100);
    }

    private static double evidenceWeight(Evidence evidence) {
        if (evidence == null) return 0;
        return switch (evidence) {
            case CONFIRMED -> 1;
            case WEAK -> .6;
            case EXPLICIT -> .5;
            case MISSING -> 0;
        };
    }

    public record AtsScore(int total, int technologyCoverage, int marketRelevance) {
    }

    public AtsAnalysis details(String text, LlmResumeAnalysisResponse llm, AtsScore legacy) {
        int parsing = parsing(text);
        int contacts = contactScore(text);
        int experience = fieldScore(llm, "experience", llm.experienceAssessment().experienceDescriptionQuality().score() * 10);
        int education = fieldScore(llm, "education", 50);
        int skills = fieldScore(llm, "skills", legacy.technologyCoverage());
        int sections = llm.atsFields().isEmpty() ? llm.resumeAssessment().atsReadability().score() * 10
                : clamp(llm.atsFields().stream().mapToInt(value -> statusScore(value.status())).average().orElse(0));
        int keywordCoverage = legacy.marketRelevance();
        double[] weights = scoring.atsWeights();
        int[] scores = {parsing, sections, contacts, experience, education, skills, keywordCoverage};
        String[] names = {"parsing", "sections", "contacts", "experience", "education", "skills", "keywordCoverage"};
        int total = clamp(java.util.stream.IntStream.range(0, scores.length)
                .mapToDouble(index -> scores[index] * weights[index]).sum());
        var components = java.util.stream.IntStream.range(0, scores.length)
                .mapToObj(index -> new ScoreBreakdown.ScoreComponent(names[index], scores[index], weights[index],
                        scores[index] * weights[index], "ATS diagnostic component")).toList();
        List<AtsAnalysis.FieldDiagnostic> diagnostics = llm.atsFields().stream()
                .map(value -> new AtsAnalysis.FieldDiagnostic(value.field(),
                        AtsAnalysis.FieldStatus.valueOf(value.status().name()), value.issues())).toList();
        return new AtsAnalysis(total, parsing, sections, contacts, experience, education, skills, keywordCoverage,
                diagnostics, new ScoreBreakdown(total, components));
    }

    private static int fieldScore(LlmResumeAnalysisResponse llm, String field, int fallback) {
        return llm.atsFields().stream().filter(value -> value.field().equalsIgnoreCase(field)).findFirst()
                .map(value -> statusScore(value.status())).orElse(fallback);
    }

    private static int statusScore(LlmResumeAnalysisResponse.AtsFieldStatus value) {
        return switch (value) { case OK -> 100; case PARTIAL -> 60; case FAILED -> 0; };
    }

    private static int parsing(String text) {
        if (text == null || text.isBlank()) return 0;
        long broken = text.chars().filter(value -> value == '\uFFFD').count();
        double brokenShare = (double) broken / text.length();
        int veryShortLines = (int) text.lines().filter(line -> !line.isBlank() && line.trim().length() < 3).count();
        return clamp(100 - brokenShare * 1000 - Math.min(30, veryShortLines));
    }

    private static int contactScore(String text) {
        if (text == null) return 0;
        boolean email = java.util.regex.Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[\\p{L}]{2,}",
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text).find();
        boolean phone = java.util.regex.Pattern.compile("(?:\\+?\\d[\\s()-]*){10,}").matcher(text).find();
        return email && phone ? 100 : email || phone ? 60 : 0;
    }

    private static int clamp(double value) { return (int) Math.clamp(Math.round(value), 0, 100); }
}
