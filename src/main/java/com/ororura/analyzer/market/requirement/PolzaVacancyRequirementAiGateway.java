package com.ororura.analyzer.market.requirement;

import java.util.List;

import com.ororura.analyzer.integration.polza.PolzaClient;
import com.ororura.analyzer.integration.polza.PolzaDtos.CompletionMessage;
import com.ororura.analyzer.integration.polza.PolzaDtos.CompletionRequest;
import com.ororura.analyzer.integration.polza.PolzaDtos.JsonSchema;
import com.ororura.analyzer.integration.polza.PolzaDtos.ResponseFormat;
import org.springframework.stereotype.Component;
import tools.jackson.databind.node.StringNode;

@Component
class PolzaVacancyRequirementAiGateway implements VacancyRequirementAiGateway {

    private final PolzaClient client;

    PolzaVacancyRequirementAiGateway(PolzaClient client) {
        this.client = client;
    }

    @Override
    public String complete(VacancyRequirementPrompt prompt) {
        CompletionRequest request = new CompletionRequest(
                List.of(new CompletionMessage("system", StringNode.valueOf(prompt.systemInstruction())),
                        new CompletionMessage("user", prompt.input())),
                new ResponseFormat("json_schema",
                        new JsonSchema("vacancy_requirement_extraction", true, prompt.schema())),
                0,
                List.of());
        return client.requestCompletion(request);
    }
}
