package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.market.requirement.MarketRequirementStatistics;
import org.springframework.stereotype.Component;

@Component
class CriterionWeightCalculator {

    private final CriterionIdFactory idFactory;

    CriterionWeightCalculator(CriterionIdFactory idFactory) {
        this.idFactory = idFactory;
    }

    List<AnalysisCriterion> calculate(List<GeneratedCriterionGroup> groups,
            MarketRequirementStatistics statistics) {
        if (statistics.processedCount() <= 0) {
            throw new MarketCriterionGenerationException("Cannot calculate coverage for an empty market sample");
        }
        List<Double> coverages = groups.stream().map(group -> coverage(group, statistics)).toList();
        double totalCoverage = coverages.stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(totalCoverage) || totalCoverage <= 0) {
            throw new MarketCriterionGenerationException("Generated criteria have zero total market coverage");
        }
        List<AnalysisCriterion> criteria = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            GeneratedCriterionGroup group = groups.get(index);
            double coverage = coverages.get(index);
            criteria.add(new AnalysisCriterion(idFactory.create(group.label(), group.requirementIds()),
                    group.label(), group.description(), coverage / totalCoverage, coverage));
        }
        return List.copyOf(criteria);
    }

    private static double coverage(GeneratedCriterionGroup group, MarketRequirementStatistics statistics) {
        Set<String> ids = new HashSet<>(group.requirementIds());
        long matchingVacancies = statistics.vacancyRequirements().stream()
                .filter(vacancy -> vacancy.requirements().stream()
                        .anyMatch(requirement -> ids.contains(requirement.requirementId())))
                .count();
        return (double) matchingVacancies / statistics.processedCount();
    }
}
