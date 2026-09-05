package com.ororura.analyzer.analysis.market;

import java.util.List;
import com.ororura.analyzer.analysis.domain.MarketSegment;
import com.ororura.analyzer.vacancy.model.Vacancy;
import com.ororura.analyzer.vacancy.search.VacancySearchCriteria;

public interface VacancySearchStrategy {
    String version();
    List<VacancySearchCriteria> plan(MarketSegment segment);
    boolean matchesDirection(MarketSegment segment, Vacancy vacancy);
}
