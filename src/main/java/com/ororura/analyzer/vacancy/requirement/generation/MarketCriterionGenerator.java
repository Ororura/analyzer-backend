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
import com.ororura.analyzer.resume.market.MarketSkillStatistics;
import com.ororura.analyzer.resume.market.MarketVacancyRequirements;
import com.ororura.analyzer.resume.market.SkillImportance;
import com.ororura.analyzer.resume.config.ResumeScoringProperties;
import com.ororura.analyzer.vacancy.requirement.MarketRequirementStatistics;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

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
    private final ResumeScoringProperties scoring;

    @Autowired
    MarketCriterionGenerator(ResumeAnalysisProfileRegistry profileRegistry,
            MarketCriterionPromptFactory promptFactory, MarketCriterionAiGateway aiGateway,
            MarketCriterionResponseParser responseParser, MarketCriterionGroupingValidator groupingValidator,
            CriterionWeightCalculator weightCalculator, MarketAnalysisProfileFactory profileFactory, Clock clock,
            ResumeScoringProperties scoring) {
        this.profileRegistry = profileRegistry;
        this.promptFactory = promptFactory;
        this.aiGateway = aiGateway;
        this.responseParser = responseParser;
        this.groupingValidator = groupingValidator;
        this.weightCalculator = weightCalculator;
        this.profileFactory = profileFactory;
        this.clock = clock;
        this.scoring = scoring;
    }

    MarketCriterionGenerator(ResumeAnalysisProfileRegistry profileRegistry,
            MarketCriterionPromptFactory promptFactory, MarketCriterionAiGateway aiGateway,
            MarketCriterionResponseParser responseParser, MarketCriterionGroupingValidator groupingValidator,
            CriterionWeightCalculator weightCalculator, MarketAnalysisProfileFactory profileFactory, Clock clock) {
        this(profileRegistry, promptFactory, aiGateway, responseParser, groupingValidator, weightCalculator,
                profileFactory, clock, new ResumeScoringProperties());
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
        List<MarketSkillStatistics> skillStatistics = statistics.requirements().stream()
                .map(value -> new MarketSkillStatistics(normalizedSkill(value.id()), value.label(),
                        (long) value.totalVacancyCount(), (long) statistics.sampleSize(), value.frequency(),
                        value.requiredFrequency(), value.preferredFrequency(), value.optionalFrequency(),
                        importance(value.frequency())))
                .toList();
        List<MarketVacancyRequirements> vacancyRequirements = statistics.vacancyRequirements().stream()
                .map(value -> new MarketVacancyRequirements(value.vacancyId(), value.requirements().stream()
                        .map(item -> new MarketVacancyRequirements.Requirement(
                                item.requirementId(), item.importance())).toList()))
                .toList();
        return profileFactory.create(statistics.profile(), definition.targetRole(), criteria, requirements,
                skillStatistics, vacancyRequirements, statistics.sufficientSample(),
                statistics.processedCount(), clock.instant());
    }

    /** Semantic grouping only; the caller applies a separately pinned Java scoring policy. */
    public List<GeneratedCriterionGroup> group(String targetRole, MarketRequirementStatistics statistics) {
        if (!statistics.sufficientSample() || statistics.requirements().isEmpty()) return List.of();
        return groupingValidator.validate(responseParser.parse(aiGateway.complete(
                promptFactory.create(targetRole, statistics))), statistics);
    }

    private SkillImportance importance(double frequency) {
        if (frequency >= scoring.getCoreFrequency()) return SkillImportance.CORE;
        if (frequency >= scoring.getHighFrequency()) return SkillImportance.HIGH;
        if (frequency >= scoring.getMediumFrequency()) return SkillImportance.MEDIUM;
        return SkillImportance.LOW;
    }

    private static String normalizedSkill(String requirementId) {
        int separator = requirementId.indexOf(':');
        String value = separator >= 0 ? requirementId.substring(separator + 1) : requirementId;
        return value.replace('-', '_');
    }
}
