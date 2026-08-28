package com.ororura.analyzer.polza;

import java.util.List;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiResumeAnalysisResponse parse(String content) {
        String json = extractJsonObject(content);
        if (json == null) {
            throw new AiResponseValidationException(
                    "Не удалось обработать результат анализа",
                    List.of("В ответе не найден JSON-объект"));
        }
        try {
            return objectMapper.readValue(json, AiResumeAnalysisResponse.class);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new AiResponseValidationException(
                    "Не удалось обработать результат анализа",
                    List.of("AI response JSON does not match the expected schema"));
        }
    }

    String extractJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) return null;
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = start; index < content.length(); index++) {
            char character = content.charAt(index);
            if (quoted) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') quoted = false;
                continue;
            }
            if (character == '"') quoted = true;
            else if (character == '{') depth++;
            else if (character == '}' && --depth == 0) return content.substring(start, index + 1);
        }
        return null;
    }
}
