package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.List;

import com.ororura.analyzer.integration.polza.PolzaClient;
import com.ororura.analyzer.integration.polza.PolzaDtos.CompletionMessage;
import com.ororura.analyzer.integration.polza.PolzaDtos.CompletionRequest;
import com.ororura.analyzer.integration.polza.PolzaDtos.JsonSchema;
import com.ororura.analyzer.integration.polza.PolzaDtos.ResponseFormat;
import org.springframework.stereotype.Component;
import tools.jackson.databind.node.StringNode;

@Component
class PolzaMarketCriterionAiGateway implements MarketCriterionAiGateway {

    private final PolzaClient client;

    PolzaMarketCriterionAiGateway(PolzaClient client) {
        this.client = client;
    }

    @Override
    public String complete(MarketCriterionPrompt prompt) {
        return client.requestCompletion(new CompletionRequest(
                List.of(new CompletionMessage("system", StringNode.valueOf(prompt.systemInstruction())),
                        new CompletionMessage("user", prompt.input())),
                new ResponseFormat("json_schema",
                        new JsonSchema("market_criterion_grouping", true, prompt.schema())),
                0,
                List.of()));
    }
}
