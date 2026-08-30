package com.ororura.analyzer.vacancy.requirement.generation;

import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileStore;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics;
import org.springframework.stereotype.Service;

@Service
public class MarketAnalysisProfileGenerationService {

    private final MarketCriterionGenerator generator;
    private final MarketAnalysisProfileStore store;

    public MarketAnalysisProfileGenerationService(MarketCriterionGenerator generator,
            MarketAnalysisProfileStore store) {
        this.generator = generator;
        this.store = store;
    }

    public MarketAnalysisProfile generateAndPublish(MarketRequirementStatistics statistics) {
        MarketAnalysisProfile completeProfile = generator.generate(statistics);
        store.publish(completeProfile);
        return completeProfile;
    }
}
