package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

public class MarketCriterionGenerationException extends RuntimeException {

    public MarketCriterionGenerationException(String message) {
        super(message);
    }

    public MarketCriterionGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
