package com.ororura.analyzer.ats;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

import static com.ororura.analyzer.ats.AtsModels.*;
import static com.ororura.analyzer.ats.AtsScoringProperties.*;

@Component
public class AtsScorer {

    public int clampScore(double value) {
        return (int) Math.clamp(Math.round(value), 0, 100);
    }

    public int calculateStructuredFiltersScore(StructuredFilters filters) {
        List<Integer> knownWeights = filters.values().stream()
                .map(FilterAssessment::status)
                .filter(status -> status != FilterStatus.UNKNOWN)
                .map(this::filterStatusWeight)
                .toList();
        if (knownWeights.isEmpty()) {
            return NO_KNOWN_FILTERS_SCORE;
        }
        return clampScore(knownWeights.stream().mapToInt(Integer::intValue).average().orElseThrow());
    }

    public int calculateAtsScore(
            int hhSearchMatch,
            int hhStructuredFilters,
            int vacancyMatch,
            int keywordCoverage,
            int recruiterReadability) {
        return clampScore(
                hhSearchMatch * HH_SEARCH_MATCH_WEIGHT
                        + hhStructuredFilters * HH_STRUCTURED_FILTERS_WEIGHT
                        + vacancyMatch * VACANCY_MATCH_WEIGHT
                        + keywordCoverage * KEYWORD_COVERAGE_WEIGHT
                        + recruiterReadability * RECRUITER_READABILITY_WEIGHT);
    }

    public int calculateKeywordCoverage(List<RawTechnologyAssessment> technologies, DetectedLevel level) {
        return weightedTechnologyCoverage(technologies, level, technology -> 1.0);
    }

    public int calculateVacancyMatch(
            List<RawTechnologyAssessment> technologies,
            DetectedLevel level,
            int targetLevelFit,
            VacancyMarketData market) {
        int skillCoverage = weightedTechnologyCoverage(
                technologies,
                level,
                technology -> market.skillFrequencies().getOrDefault(technology, 0.0));
        return clampScore(skillCoverage * VACANCY_SKILL_COVERAGE_WEIGHT + targetLevelFit * TARGET_LEVEL_FIT_WEIGHT);
    }

    public AtsAnalysisResult finalizeAtsAnalysis(RawAtsAnalysis raw, VacancyMarketData market) {
        List<TechnologyAssessment> technologies = raw.technologies().stream()
                .map(assessment -> {
                    String technology = canonicalTechnology(assessment.technology());
                    TechnologyTier tier = TECHNOLOGY_TIERS.getOrDefault(technology, TechnologyTier.BONUS);
                    return new TechnologyAssessment(technology, assessment.status(), assessment.evidence(), tier);
                })
                .toList();
        List<RawTechnologyAssessment> normalizedTechnologies = technologies.stream()
                .map(item -> new RawTechnologyAssessment(item.technology(), item.status(), item.evidence()))
                .toList();

        int structuredFilters = calculateStructuredFiltersScore(raw.structuredFilters());
        int keywordCoverage = calculateKeywordCoverage(normalizedTechnologies, raw.detectedLevel());
        int vacancyMatch = calculateVacancyMatch(normalizedTechnologies, raw.detectedLevel(), raw.targetLevelFit(), market);
        int atsScore = calculateAtsScore(raw.hhSearchMatch(), structuredFilters, vacancyMatch, keywordCoverage,
                raw.recruiterReadability());

        Keywords keywords = new Keywords(
                names(technologies, EnumSet.of(TechnologyStatus.CONFIRMED_EXPERIENCE,
                        TechnologyStatus.EXPLICIT_OTHER, TechnologyStatus.SKILLS_ONLY), null),
                names(technologies, EnumSet.of(TechnologyStatus.SEMANTIC_EXPERIENCE), null),
                names(technologies, EnumSet.of(TechnologyStatus.CONFIRMED_EXPERIENCE,
                        TechnologyStatus.SEMANTIC_EXPERIENCE), null),
                names(technologies, EnumSet.of(TechnologyStatus.SKILLS_ONLY), null),
                names(technologies, EnumSet.of(TechnologyStatus.MISSING), TechnologyTier.CORE),
                names(technologies, EnumSet.of(TechnologyStatus.MISSING), TechnologyTier.COMMON),
                names(technologies, EnumSet.of(TechnologyStatus.MISSING), TechnologyTier.BONUS),
                names(technologies, EnumSet.of(TechnologyStatus.IRRELEVANT), null));

        return new AtsAnalysisResult(
                raw.hhSearchMatch(), raw.recruiterReadability(), raw.detectedLevel(), raw.targetLevelFit(),
                raw.experience(), raw.structuredFilters(), technologies, raw.scoreEvidence(), raw.strengths(),
                raw.weaknesses(), raw.recruiterRisks(), raw.recommendations(), raw.summary(), atsScore,
                structuredFilters, vacancyMatch, keywordCoverage, getScreeningChance(atsScore), keywords,
                new MarketData(market.source(), market.sampleSize(), market.warnings()));
    }

    public ScreeningChance getScreeningChance(int score) {
        if (score < 40) return ScreeningChance.LOW;
        if (score < 55) return ScreeningChance.BELOW_AVERAGE;
        if (score < 70) return ScreeningChance.MEDIUM;
        if (score < 85) return ScreeningChance.HIGH;
        return ScreeningChance.VERY_HIGH;
    }

    private int weightedTechnologyCoverage(
            List<RawTechnologyAssessment> technologies,
            DetectedLevel level,
            ToDoubleFunction<String> frequency) {
        Map<String, RawTechnologyAssessment> assessments = new LinkedHashMap<>();
        technologies.forEach(item -> assessments.put(canonicalTechnology(item.technology()), item));

        double earned = 0;
        double available = 0;
        for (Map.Entry<String, TechnologyTier> entry : TECHNOLOGY_TIERS.entrySet()) {
            double marketWeight = frequency.applyAsDouble(entry.getKey());
            if (marketWeight <= 0) continue;
            RawTechnologyAssessment assessment = assessments.get(entry.getKey());
            if (assessment != null && assessment.status() == TechnologyStatus.IRRELEVANT) continue;
            double weight = tierWeight(entry.getValue(), level) * marketWeight;
            available += weight;
            earned += weight * (assessment == null ? 0 : TECHNOLOGY_STATUS_WEIGHT.get(assessment.status()));
        }
        return available == 0 ? 0 : clampScore((earned / available) * 100);
    }

    private double tierWeight(TechnologyTier tier, DetectedLevel level) {
        return switch (tier) {
            case CORE -> 5;
            case COMMON -> 2;
            case BONUS -> EnumSet.of(DetectedLevel.INTERN, DetectedLevel.JUNIOR, DetectedLevel.JUNIOR_PLUS)
                    .contains(level) ? 0.5 : 1;
        };
    }

    private int filterStatusWeight(FilterStatus status) {
        return switch (status) {
            case MATCH -> FILTER_MATCH;
            case PARTIAL -> FILTER_PARTIAL;
            case MISMATCH -> FILTER_MISMATCH;
            case UNKNOWN -> throw new IllegalArgumentException("unknown has no numeric weight");
        };
    }

    private List<String> names(
            List<TechnologyAssessment> technologies,
            EnumSet<TechnologyStatus> statuses,
            TechnologyTier tier) {
        List<String> names = new ArrayList<>();
        for (TechnologyAssessment item : technologies) {
            if (statuses.contains(item.status()) && (tier == null || item.tier() == tier)) {
                names.add(item.technology());
            }
        }
        return List.copyOf(names);
    }
}
