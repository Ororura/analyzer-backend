package com.ororura.analyzer.ats.scoring;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import com.ororura.analyzer.ats.domain.AtsScores;
import com.ororura.analyzer.ats.domain.AtsScores.ScreeningChance;
import com.ororura.analyzer.ats.domain.CandidateAssessment.DetectedLevel;
import com.ororura.analyzer.ats.domain.MatchingAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.FilterAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.FilterStatus;
import com.ororura.analyzer.ats.domain.MatchingAssessment.Keywords;
import com.ororura.analyzer.ats.domain.MatchingAssessment.ResumeFilterAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyAssessment;
import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyStatus;
import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyTier;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis.NormalizedTechnologyAssessment;
import com.ororura.analyzer.ats.domain.ResumeAtsAnalysis;
import com.ororura.analyzer.ats.market.MarketSummary;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

import static com.ororura.analyzer.ats.scoring.AtsScoringProperties.*;

@Component
public class AtsScorer {

    public int clampScore(double value) {
        return Math.clamp(Math.round(value), 0, 100);
    }

    public int calculateStructuredFiltersScore(ResumeFilterAssessment filters) {
        List<Integer> knownWeights = Stream.of(
                        filters.experience(), filters.education(), filters.location(), filters.relocation(),
                        filters.salary(), filters.languages(), filters.employmentType(), filters.workFormat())
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

    public int calculateKeywordCoverage(
            List<NormalizedTechnologyAssessment> technologies,
            DetectedLevel level) {
        return weightedTechnologyCoverage(technologies, level, technology -> 1.0);
    }

    public int calculateVacancyMatch(
            List<NormalizedTechnologyAssessment> technologies,
            DetectedLevel level,
            int targetLevelFit,
            VacancyMarketData market) {
        int skillCoverage = weightedTechnologyCoverage(
                technologies,
                level,
                technology -> market.skillFrequencies().getOrDefault(technology, 0.0));
        return clampScore(skillCoverage * VACANCY_SKILL_COVERAGE_WEIGHT + targetLevelFit * TARGET_LEVEL_FIT_WEIGHT);
    }

    public ResumeAtsAnalysis finalizeAtsAnalysis(NormalizedAtsAnalysis normalized, VacancyMarketData market) {
        List<TechnologyAssessment> technologies = normalized.technologies().stream()
                .map(assessment -> {
                    String technology = canonicalTechnology(assessment.technology());
                    TechnologyTier tier = TECHNOLOGY_TIERS.getOrDefault(technology, TechnologyTier.BONUS);
                    return new TechnologyAssessment(technology, assessment.status(), assessment.evidence(), tier);
                })
                .toList();
        List<NormalizedTechnologyAssessment> canonicalTechnologies = technologies.stream()
                .map(item -> new NormalizedTechnologyAssessment(item.technology(), item.status(), item.evidence()))
                .toList();

        int structuredFilters = calculateStructuredFiltersScore(normalized.filters());
        int keywordCoverage = calculateKeywordCoverage(canonicalTechnologies, normalized.candidate().detectedLevel());
        int vacancyMatch = calculateVacancyMatch(canonicalTechnologies, normalized.candidate().detectedLevel(),
                normalized.targetLevelFit().score(), market);
        int atsScore = calculateAtsScore(normalized.hhSearchMatch().score(), structuredFilters, vacancyMatch,
                keywordCoverage, normalized.recruiterReadability().score());

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

        return new ResumeAtsAnalysis(
                normalized.candidate(),
                new MatchingAssessment(normalized.filters(), technologies, keywords),
                new AtsScores(normalized.hhSearchMatch(), normalized.recruiterReadability(),
                        normalized.targetLevelFit(), atsScore, structuredFilters, vacancyMatch, keywordCoverage,
                        getScreeningChance(atsScore)),
                normalized.insights(),
                new MarketSummary(market.source(), market.sampleSize(), market.warnings()));
    }

    public ScreeningChance getScreeningChance(int score) {
        if (score < 40) return ScreeningChance.LOW;
        if (score < 55) return ScreeningChance.BELOW_AVERAGE;
        if (score < 70) return ScreeningChance.MEDIUM;
        if (score < 85) return ScreeningChance.HIGH;
        return ScreeningChance.VERY_HIGH;
    }

    private int weightedTechnologyCoverage(
            List<NormalizedTechnologyAssessment> technologies,
            DetectedLevel level,
            ToDoubleFunction<String> frequency) {
        Map<String, NormalizedTechnologyAssessment> assessments = new LinkedHashMap<>();
        technologies.forEach(item -> assessments.put(canonicalTechnology(item.technology()), item));

        double earned = 0;
        double available = 0;
        for (Map.Entry<String, TechnologyTier> entry : TECHNOLOGY_TIERS.entrySet()) {
            double marketWeight = frequency.applyAsDouble(entry.getKey());
            if (marketWeight <= 0) continue;
            NormalizedTechnologyAssessment assessment = assessments.get(entry.getKey());
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
