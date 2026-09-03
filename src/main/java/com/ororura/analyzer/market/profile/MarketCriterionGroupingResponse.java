package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.List;

record MarketCriterionGroupingResponse(List<GeneratedCriterionGroup> criteria) {

    MarketCriterionGroupingResponse {
        if (criteria == null) {
            throw new IllegalArgumentException("Market criterion response must define criteria");
        }
        criteria = List.copyOf(criteria);
    }
}
