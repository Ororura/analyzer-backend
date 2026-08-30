package com.ororura.analyzer.vacancy.requirement.generation;

import java.util.List;

import com.ororura.analyzer.polza.PolzaClient;
import com.ororura.analyzer.polza.PolzaDtos.CompletionMessage;
import com.ororura.analyzer.polza.PolzaDtos.CompletionRequest;
import com.ororura.analyzer.polza.PolzaDtos.JsonSchema;
import com.ororura.analyzer.polza.PolzaDtos.ResponseFormat;
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
