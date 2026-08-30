package com.ororura.analyzer.vacancy.requirement.generation;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;

import com.ororura.analyzer.resume.ai.AnalysisCriterion;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileDefinition;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileFactory;
import com.ororura.analyzer.resume.market.MarketRequirement;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics;
import org.springframework.stereotype.Component;

@Component
public class MarketCriterionGenerator {

    private final ResumeAnalysisProfileRegistry profileRegistry;
    private final MarketCriterionPromptFactory promptFactory;
    private final MarketCriterionAiGateway aiGateway;
    private final MarketCriterionResponseParser responseParser;
    private final MarketCriterionGroupingValidator groupingValidator;
    private final CriterionWeightCalculator weightCalculator;
    private final MarketAnalysisProfileFactory profileFactory;
    private final Clock clock;

    MarketCriterionGenerator(ResumeAnalysisProfileRegistry profileRegistry,
            MarketCriterionPromptFactory promptFactory, MarketCriterionAiGateway aiGateway,
            MarketCriterionResponseParser responseParser, MarketCriterionGroupingValidator groupingValidator,
            CriterionWeightCalculator weightCalculator, MarketAnalysisProfileFactory profileFactory, Clock clock) {
        this.profileRegistry = profileRegistry;
        this.promptFactory = promptFactory;
        this.aiGateway = aiGateway;
        this.responseParser = responseParser;
        this.groupingValidator = groupingValidator;
        this.weightCalculator = weightCalculator;
        this.profileFactory = profileFactory;
        this.clock = clock;
    }

    public MarketAnalysisProfile generate(MarketRequirementStatistics statistics) {
        if (statistics == null || !statistics.sufficientSample()) {
            throw new MarketCriterionGenerationException("Market sample is insufficient for criterion generation");
        }
        if (statistics.requirements().isEmpty()) {
            throw new MarketCriterionGenerationException("Market requirements are empty");
        }
        ResumeAnalysisProfileDefinition definition = profileRegistry.get(statistics.profile());
        MarketCriterionPrompt prompt = promptFactory.create(definition.targetRole(), statistics);
        List<GeneratedCriterionGroup> groups = groupingValidator.validate(
                responseParser.parse(aiGateway.complete(prompt)), statistics);
        List<AnalysisCriterion> criteria = weightCalculator.calculate(groups, statistics).stream()
                .sorted(Comparator.comparing(AnalysisCriterion::id)).toList();
        List<MarketRequirement> requirements = statistics.requirements().stream()
                .map(MarketRequirementStatistics.RequirementStatistics::requirement)
                .sorted(Comparator.comparing(MarketRequirement::id)).toList();
        return profileFactory.create(statistics.profile(), definition.targetRole(), criteria, requirements,
                statistics.processedCount(), clock.instant());
    }
}
