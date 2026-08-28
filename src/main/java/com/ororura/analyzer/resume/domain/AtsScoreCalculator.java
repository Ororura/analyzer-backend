package com.ororura.analyzer.resume.domain;

import java.util.Map;

import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.Evidence;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.Tier;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class AtsScoreCalculator {

    private final TechnologyTaxonomy taxonomy;

    public AtsScoreCalculator(TechnologyTaxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    public AtsScore calculate(int atsReadability, int resumeQuality, int experienceDescription,
            int commercialExperience, TechnologyProfile technologies, VacancyMarketData market) {
        int coverage = coverage(technologies);
        int relevance = marketRelevance(technologies, market.skillFrequencies());
        int total = ScoreMath.score(atsReadability * 10 * .25 + coverage * .25 + relevance * .20
                + resumeQuality * 10 * .15 + experienceDescription * 10 * .10
                + commercialExperience * 10 * .05);
        return new AtsScore(total, coverage, relevance);
    }

    int coverage(TechnologyProfile profile) {
        return weighted(profile, technology -> 1.0);
    }

    int marketRelevance(TechnologyProfile profile, Map<String, Double> frequencies) {
        return weighted(profile, technology -> frequencies.getOrDefault(technology, 0.0));
    }

    private int weighted(TechnologyProfile profile, Weight weight) {
        double earned = 0;
        double available = 0;
        for (Map.Entry<String, Tier> technology : taxonomy.tiers().entrySet()) {
            double currentWeight = tierWeight(technology.getValue()) * weight.value(technology.getKey());
            if (currentWeight <= 0) continue;
            available += currentWeight;
            earned += currentWeight * evidenceWeight(profile.evidence().get(technology.getKey()));
        }
        return available == 0 ? 0 : ScoreMath.score(earned / available * 100);
    }

    private static double tierWeight(Tier tier) {
        return switch (tier) {
            case CORE -> 5;
            case COMMON -> 2;
            case BONUS -> .5;
        };
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

    private interface Weight {
        double value(String technology);
    }

    public record AtsScore(int total, int technologyCoverage, int marketRelevance) {
    }
}
