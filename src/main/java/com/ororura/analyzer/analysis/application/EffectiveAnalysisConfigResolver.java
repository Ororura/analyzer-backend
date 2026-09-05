package com.ororura.analyzer.analysis.application;

import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.resume.ai.*;
import com.ororura.analyzer.resume.config.ResumeScoringProperties;
import com.ororura.analyzer.resume.market.*;
import com.ororura.analyzer.vacancy.market.*;

@Service
public class EffectiveAnalysisConfigResolver {
    private final ResumeScoringProperties scoring;
    private final AiProviderRegistry providers;
    private final Clock clock;
    public EffectiveAnalysisConfigResolver(ResumeScoringProperties scoring,AiProviderRegistry providers,Clock clock) {
        this.scoring=scoring;this.providers=providers;this.clock=clock;
    }
    public EffectiveAnalysisConfig resolve(UserAnalysisProfile profile,MarketSnapshot snapshot,AiProviderType requested) {
        if(!profile.segment().equals(snapshot.segment())) throw new IllegalArgumentException("Snapshot segment mismatch");
        var provider=providers.get(requested);
        var policy=new ScoringPolicy(ScoringPolicy.VERSION,GradePolicy.VERSION,scoring.snapshotParameters());
        var stats=snapshot.statistics();
        var criteria=new ArrayList<AnalysisCriterion>();
        double sum=snapshot.criteria().stream().mapToDouble(group -> coverage(group.requirementIds(),snapshot)).sum();
        for(int i=0;i<snapshot.criteria().size();i++) {
            var group=snapshot.criteria().get(i);
            double frequency=coverage(group.requirementIds(),snapshot);
            criteria.add(new AnalysisCriterion("market_"+i,group.label(),group.description(),
                    sum==0 ? 1.0/snapshot.criteria().size() : frequency/sum,frequency));
        }
        if(criteria.isEmpty()) criteria.add(new AnalysisCriterion("role_evidence",profile.specialization()+" evidence",
                "Evaluate concrete tasks, technical depth and outcomes relevant to "+profile.segment().targetRole(),1,0));
        var requirements=stats.requirements().stream().map(v -> v.requirement()).toList();
        var skills=stats.requirements().stream().map(v -> new MarketSkillStatistics(
                v.id().substring(v.id().indexOf(':')+1).replace('-','_'),v.label(),(long)v.totalVacancyCount(),
                (long)stats.processedCount(),v.frequency(),v.requiredFrequency(),v.preferredFrequency(),v.optionalFrequency(),
                importance(v.frequency()))).toList();
        var mappings=stats.vacancyRequirements().stream().map(v -> new MarketVacancyRequirements(v.vacancyId(),
                v.requirements().stream().map(r -> new MarketVacancyRequirements.Requirement(r.requirementId(),r.importance())).toList())).toList();
        var analysis=new MarketAnalysisProfile(null,profile.segment().targetRole(),criteria,requirements,skills,mappings,
                stats.sufficientSample(),stats.processedCount(),snapshot.generatedAt(),snapshot.id().toString());
        Map<String,Double> frequencies=new LinkedHashMap<>(); requirements.forEach(r -> frequencies.put(r.label(),r.frequency()));
        Map<String,Integer> experience=new LinkedHashMap<>(), employment=new LinkedHashMap<>(),formats=new LinkedHashMap<>();
        Set<String> processed=stats.vacancyRequirements().stream().map(v -> v.vacancyId()).collect(java.util.stream.Collectors.toSet());
        snapshot.vacancies().stream().filter(v -> processed.contains(v.vacancy().id())).forEach(v -> {
            increment(experience,v.vacancy().experience());increment(employment,v.vacancy().employment());increment(formats,v.vacancy().workFormat());
        });
        var market=new VacancyMarketData("snapshot",stats.processedCount(),frequencies,experience,employment,formats,
                null,requirements.stream().map(MarketRequirement::label).toList(),List.of(),skills,snapshot.id().toString(),
                stats.sufficientSample(),snapshot.warnings());
        return new EffectiveAnalysisConfig(1,profile,snapshot.id(),policy,analysis,market,provider.type(),
                provider.model().orElse(null),"resume-prompt-v1",clock.instant());
    }
    private static double coverage(List<String> ids,MarketSnapshot snapshot) {
        if(snapshot.statistics().processedCount()==0) return 0;
        long count=snapshot.statistics().vacancyRequirements().stream()
                .filter(v -> v.requirements().stream().anyMatch(r -> ids.contains(r.requirementId()))).count();
        return (double)count/snapshot.statistics().processedCount();
    }
    private SkillImportance importance(double value) {
        if(value>=scoring.getCoreFrequency()) return SkillImportance.CORE;
        if(value>=scoring.getHighFrequency()) return SkillImportance.HIGH;
        if(value>=scoring.getMediumFrequency()) return SkillImportance.MEDIUM;
        return SkillImportance.LOW;
    }
    private static void increment(Map<String,Integer> map,String value) {
        if(value!=null && !value.isBlank()) map.merge(value,1,Integer::sum);
    }
}
