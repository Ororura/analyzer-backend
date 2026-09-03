package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

interface MarketCriterionAiGateway {

    String complete(MarketCriterionPrompt prompt);
}
