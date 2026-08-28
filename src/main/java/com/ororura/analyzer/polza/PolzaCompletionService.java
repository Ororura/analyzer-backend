package com.ororura.analyzer.polza;

import java.util.List;

import tools.jackson.databind.node.StringNode;
import org.springframework.stereotype.Service;

import static com.ororura.analyzer.polza.PolzaDtos.*;

@Service
public class PolzaCompletionService {

    private static final String REPAIR_SYSTEM_PROMPT =
            "Исправь только JSON-структуру по переданной schema. Не добавляй новые факты. "
                    + "Для отсутствующей информации используй null/unknown/пустой массив. Верни только JSON.";

    private final PolzaClient client;
    private final AiResponseParser parser;

    public PolzaCompletionService(PolzaClient client, AiResponseParser parser) {
        this.client = client;
        this.parser = parser;
    }

    public AiResumeAnalysisResponse requestAiResult(CompletionRequest initialRequest) {
        String initialContent = client.requestCompletion(initialRequest);
        try {
            return parser.parse(initialContent);
        } catch (AiResponseValidationException initialFailure) {
            CompletionRequest repairRequest = buildRepairRequest(
                    initialRequest.responseFormat(), initialContent, initialFailure.getIssues());
            String repairedContent = client.requestCompletion(repairRequest);
            return parser.parse(repairedContent);
        }
    }

    private CompletionRequest buildRepairRequest(
            ResponseFormat responseFormat,
            String invalidResponse,
            List<String> issues) {
        return new CompletionRequest(
                List.of(
                        new CompletionMessage("system", StringNode.valueOf(REPAIR_SYSTEM_PROMPT)),
                        new CompletionMessage("user", StringNode.valueOf(
                                "INVALID_RESPONSE:\n" + invalidResponse + "\n\nVALIDATION_ERRORS:\n"
                                        + String.join("\n", issues)))),
                responseFormat,
                0,
                List.of());
    }
}
