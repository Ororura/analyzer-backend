package com.ororura.analyzer.analysis.market;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import com.ororura.analyzer.analysis.persistence.AnalysisProfileRepository;
import com.ororura.analyzer.vacancy.search.VacancySearchCriteria;

/** Rotates through active profiles; only market filters, never owner IDs, are sent to the provider. */
@Component
public class ProfileMarketQuerySource {
    private final AnalysisProfileRepository profiles;
    private final VacancySearchStrategy strategy;
    private int nextPage;
    public ProfileMarketQuerySource(AnalysisProfileRepository profiles,VacancySearchStrategy strategy) {
        this.profiles=profiles;this.strategy=strategy;
    }
    @Transactional(readOnly=true)
    public synchronized List<VacancySearchCriteria> nextPlans() {
        var page=profiles.findByDeletedAtIsNull(PageRequest.of(nextPage,50,Sort.by("id")));
        if(page.isEmpty() && nextPage>0) page=profiles.findByDeletedAtIsNull(PageRequest.of(0,50,Sort.by("id")));
        nextPage=page.hasNext() ? page.getNumber()+1 : 0;
        return page.stream().map(v -> v.domain().segment()).distinct().flatMap(s -> strategy.plan(s).stream()).distinct().toList();
    }
}
