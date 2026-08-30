package com.ororura.analyzer.resume.domain;

import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.Evidence;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketRequirement;
import org.springframework.stereotype.Component;

@Component
public class AtsScoreCalculator {

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
}
