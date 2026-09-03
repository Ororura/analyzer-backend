package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.application.port.MarketAnalysisProfileStore;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics;
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
