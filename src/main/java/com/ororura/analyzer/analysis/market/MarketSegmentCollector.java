package com.ororura.analyzer.analysis.market;

import java.util.*;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.vacancy.search.VacancyQueryService;
import org.springframework.stereotype.Service;

@Service
public class MarketSegmentCollector {
    private final VacancyQueryService queries;
    private final VacancySearchStrategy strategy;
    private final GradePolicy grades=new GradePolicy();
    public MarketSegmentCollector(VacancyQueryService queries,VacancySearchStrategy strategy) {
        this.queries=queries; this.strategy=strategy;
    }
    public CollectionResult collect(MarketSegment segment) {
        var plan=strategy.plan(segment);
        Set<String> seen=new HashSet<>();
        List<MarketSnapshot.SnapshotVacancy> accepted=new ArrayList<>();
        List<String> warnings=new ArrayList<>();
        Set<Integer> exhausted=new HashSet<>();
        int scanned=0; boolean truncated=false;
        // Round-robin query pages; <=200 scanned per query and <=200 accepted in total.
        outer: for(int page=0;page<4;page++) {
            for(int i=0;i<plan.size();i++) {
                if(exhausted.contains(i)) continue;
                var result=queries.searchDomain(plan.get(i).withPage(page,50));
                warnings.addAll(result.warnings());
                scanned+=result.items().size();
                if(!result.hasNext() || result.items().isEmpty()) exhausted.add(i);
                if(page==3 && result.hasNext()) truncated=true;
                for(var vacancy:result.items()) {
                    if(!seen.add(vacancy.id())) continue;
                    var grade=grades.classify(vacancy.title());
                    var match=grades.match(segment.searchGrade(),grade.vacancyGrade());
                    if(!strategy.matchesDirection(segment,vacancy)) continue;
                    if(match==GradePolicy.Match.MISMATCH || match==GradePolicy.Match.UNKNOWN && !segment.filters().includeUnknownGrade()) continue;
                    accepted.add(new MarketSnapshot.SnapshotVacancy(vacancy,grade.vacancyGrade(),grade.source()));
                    if(accepted.size()==200) { truncated=true; break outer; }
                }
            }
        }
        if(truncated) warnings.add("MARKET_SEARCH_TRUNCATED");
        return new CollectionResult(List.copyOf(accepted),plan.stream().map(v -> v.query()).toList(),
                scanned,seen.size(),truncated,warnings.stream().distinct().toList());
    }
    public record CollectionResult(List<MarketSnapshot.SnapshotVacancy> vacancies,List<String> queries,
            int scannedCount,int uniqueCount,boolean truncated,List<String> warnings) {}
}
