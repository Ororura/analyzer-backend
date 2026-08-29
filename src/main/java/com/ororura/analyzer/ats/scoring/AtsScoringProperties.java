package com.ororura.analyzer.ats.scoring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyStatus;
import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyTier;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy;

public final class AtsScoringProperties {

    private static final TechnologyTaxonomy TAXONOMY = new TechnologyTaxonomy();

    public static final Map<TechnologyStatus, Double> TECHNOLOGY_STATUS_WEIGHT = Map.of(
            TechnologyStatus.CONFIRMED_EXPERIENCE, 1.0,
            TechnologyStatus.SEMANTIC_EXPERIENCE, 0.85,
            TechnologyStatus.EXPLICIT_OTHER, 0.75,
            TechnologyStatus.SKILLS_ONLY, 0.5,
            TechnologyStatus.MISSING, 0.0,
            TechnologyStatus.IRRELEVANT, 0.0);

    public static final int FILTER_MATCH = 100;
    public static final int FILTER_PARTIAL = 60;
    public static final int FILTER_MISMATCH = 0;
    public static final int NO_KNOWN_FILTERS_SCORE = 50;

    public static final double HH_SEARCH_MATCH_WEIGHT = 0.20;
    public static final double HH_STRUCTURED_FILTERS_WEIGHT = 0.20;
    public static final double VACANCY_MATCH_WEIGHT = 0.25;
    public static final double KEYWORD_COVERAGE_WEIGHT = 0.20;
    public static final double RECRUITER_READABILITY_WEIGHT = 0.15;

    public static final double VACANCY_SKILL_COVERAGE_WEIGHT = 0.80;
    public static final double TARGET_LEVEL_FIT_WEIGHT = 0.20;

    public static final Map<String, TechnologyTier> TECHNOLOGY_TIERS = technologyTiers();

    private AtsScoringProperties() {
    }

    public static String canonicalTechnology(String value) {
        return TAXONOMY.canonical(value);
    }

    private static Map<String, TechnologyTier> technologyTiers() {
        Map<String, TechnologyTier> tiers = new LinkedHashMap<>();
        TAXONOMY.tiers().forEach((technology, tier) -> tiers.put(technology, TechnologyTier.valueOf(tier.name())));
        return Collections.unmodifiableMap(tiers);
    }
}
