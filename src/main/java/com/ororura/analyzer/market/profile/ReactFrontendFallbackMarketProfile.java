package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.List;
import java.util.Map;

import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.profile.AnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import org.springframework.stereotype.Component;

/** Last-resort bootstrap data. Frequencies are explicitly fallback defaults, not live market statistics. */
@Component
public class ReactFrontendFallbackMarketProfile implements FallbackMarketProfileDefinition {
    private final AnalysisTechnologyCatalog catalog;

    public ReactFrontendFallbackMarketProfile(AnalysisTechnologyCatalog catalog) { this.catalog = catalog; }

    @Override public ResumeAnalysisProfile profile() { return ResumeAnalysisProfile.REACT_FRONTEND; }

    @Override public List<AnalysisCriterion> criteria() {
        return List.of(criterion("javascript-fallback", "JavaScript", .15), criterion("typescript-fallback", "TypeScript", .15),
                criterion("react-fallback", "React", .25), criterion("frontend-platform-fallback", "Frontend Platform", .20),
                criterion("state-management-fallback", "State Management", .15), criterion("frontend-testing-fallback", "Testing", .10));
    }

    @Override public List<MarketRequirement> requirements() {
        Map<String, Double> frequencies = Map.of("JavaScript", .9, "TypeScript", .8, "React", .9,
                "HTML", .7, "CSS", .7, "REST API", .6, "State management", .5, "Testing", .4);
        return catalog.technologies(profile()).stream().map(value -> new MarketRequirement(
                MarketRequirementId.of(RequirementType.TECHNOLOGY, value.name()), value.name(),
                RequirementType.TECHNOLOGY, frequencies.getOrDefault(value.name(), 0.0))).toList();
    }

    private static AnalysisCriterion criterion(String id, String label, double weight) {
        return new AnalysisCriterion(id, label, "Assessment for " + label, weight, 0);
    }
}
