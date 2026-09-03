package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import tools.jackson.databind.JsonNode;

record MarketCriterionPrompt(String systemInstruction, JsonNode input, JsonNode schema) {
}
