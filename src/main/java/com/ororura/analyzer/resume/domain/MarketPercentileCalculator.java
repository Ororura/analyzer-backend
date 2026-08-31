package com.ororura.analyzer.resume.domain;

import com.ororura.analyzer.resume.api.MarketPosition;
import org.springframework.stereotype.Component;

@Component
public class MarketPercentileCalculator {
    public MarketPosition calculate() {
        return new MarketPosition(null, null, null);
    }
}
