package com.ororura.analyzer.analysis.market;

import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.vacancy.requirement.*;
import com.ororura.analyzer.vacancy.requirement.generation.*;

@Service
public class MarketSnapshotBuilder {
    private final MarketSegmentCollector collector;
    private final VacancyRequirementPipeline pipeline;
    private final MarketCriterionGenerator generator;
    private final Clock clock;
    private final MarketRequirementProperties properties;
    private final com.ororura.analyzer.polza.PolzaProperties polza;
    private final VacancySearchStrategy strategy;
    public MarketSnapshotBuilder(MarketSegmentCollector collector,VacancyRequirementPipeline pipeline,
            MarketCriterionGenerator generator,Clock clock,MarketRequirementProperties properties,
            com.ororura.analyzer.polza.PolzaProperties polza,VacancySearchStrategy strategy) {
        this.collector=collector;this.pipeline=pipeline;this.generator=generator;this.clock=clock;this.properties=properties;this.polza=polza;this.strategy=strategy;
    }
    public MarketSnapshot build(MarketSegment segment) {
        var pool=collector.collect(segment);
        var statistics=pipeline.analyzeSegment(pool.vacancies().stream().map(MarketSnapshot.SnapshotVacancy::vacancy).toList());
        List<String> warnings=new ArrayList<>(pool.warnings());
        if(!statistics.sufficientSample()) warnings.add("INSUFFICIENT_MARKET_SAMPLE");
        if(statistics.failedCount()>0) warnings.add("MARKET_REQUIREMENT_EXTRACTION_PARTIAL_FAILURE");
        List<GeneratedCriterionGroup> groups;
        try { groups=generator.group(segment.targetRole(),statistics); }
        catch(RuntimeException e) { groups=List.of(); warnings.add("MARKET_GROUPING_UNAVAILABLE"); }
        if(groups.isEmpty() && !statistics.requirements().isEmpty()) {
            // A transparent deterministic grouping fallback, without invented market frequencies.
            groups=statistics.requirements().stream().collect(java.util.stream.Collectors.groupingBy(
                    MarketRequirementStatistics.RequirementStatistics::type,TreeMap::new,java.util.stream.Collectors.toList()))
                    .entrySet().stream().map(e -> new GeneratedCriterionGroup(e.getKey().name(),
                            "Evidence of "+e.getKey().name()+" requirements",e.getValue().stream().map(v -> v.id()).toList())).toList();
        }
        return new MarketSnapshot(UUID.randomUUID(),segment,clock.instant(),"segment-market-v1",Map.of(
                "searchStrategy",strategy.version(),"gradePolicy",GradePolicy.VERSION,
                "requirementPrompt","vacancy-requirements-v1","groupingPrompt","market-grouping-v1",
                "provider","POLZA","model",Objects.toString(polza.model(),"provider-default"),
                "minimumSampleSize",Integer.toString(properties.minimumSampleSize()),
                "minVacancyCount",Integer.toString(properties.minVacancyCount()),
                "minRequirementFrequency",Double.toString(properties.minRequirementFrequency())),pool.queries(),
                pool.scannedCount(),pool.uniqueCount(),pool.truncated(),pool.vacancies(),statistics,groups,warnings);
    }
}
