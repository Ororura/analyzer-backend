package com.ororura.analyzer.ats.market;

import java.util.List;

public record MarketSummary(String source, int sampleSize, List<String> warnings) {
    public MarketSummary {
        warnings = List.copyOf(warnings);
    }
}
