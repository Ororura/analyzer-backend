package com.ororura.analyzer.vacancy.requirement.generation;

import java.util.List;

record MarketCriterionGroupingResponse(List<GeneratedCriterionGroup> criteria) {

    MarketCriterionGroupingResponse {
        if (criteria == null) {
            throw new IllegalArgumentException("Market criterion response must define criteria");
        }
        criteria = List.copyOf(criteria);
    }
}
