package com.ororura.analyzer.resume.ai;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class ResumeAnalysisSchemaFactory {

    private final ObjectMapper objectMapper;

    public ResumeAnalysisSchemaFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode create() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("technicalAssessment", scoredObject("javaDepth", "springDepth", "backendDepth",
                "sqlPostgresqlDepth", "hibernateJpaDepth", "infrastructureDepth", "messagingCacheDepth",
                "testingDepth"));
        properties.set("experienceAssessment", scoredObject("commercialRelevance", "experienceDescriptionQuality",
                "responsibilityLevel"));
        properties.set("resumeAssessment", scoredObject("resumeQuality", "atsReadability"));
        properties.set("skills", stringArraysObject("confirmed", "weakEvidence", "missing"));
        properties.set("employmentPeriods", employmentPeriods());
        properties.set("strengths", stringArray());
        properties.set("weaknesses", stringArray());
        properties.set("atsIssues", stringArray());
        properties.set("recommendations", stringArray());
        properties.set("warnings", stringArray());
        return closedRequiredObject(properties);
    }

    private ObjectNode scoredObject(String... fields) {
        ObjectNode properties = objectMapper.createObjectNode();
        for (String field : fields) {
            properties.set(field, semanticScore());
        }
        return closedRequiredObject(properties);
    }

    private ObjectNode semanticScore() {
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode score = integerType();
        score.put("minimum", 0);
        score.put("maximum", 10);
        properties.set("score", score);
        properties.set("evidence", stringArray());
        return closedRequiredObject(properties);
    }

    private ObjectNode stringArraysObject(String... fields) {
        ObjectNode properties = objectMapper.createObjectNode();
        for (String field : fields) {
            properties.set(field, stringArray());
        }
        return closedRequiredObject(properties);
    }

    private ObjectNode employmentPeriods() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("company", stringType());
        properties.set("position", stringType());
        properties.set("startYear", nullableInteger(1950, null));
        properties.set("startMonth", nullableInteger(1, 12));
        properties.set("endYear", nullableInteger(1950, null));
        properties.set("endMonth", nullableInteger(1, 12));
        ObjectNode current = objectMapper.createObjectNode();
        current.put("type", "boolean");
        properties.set("current", current);

        ObjectNode array = objectMapper.createObjectNode();
        array.put("type", "array");
        array.set("items", closedRequiredObject(properties));
        return array;
    }

    private ObjectNode closedRequiredObject(ObjectNode properties) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", "object");
        value.put("additionalProperties", false);
        value.set("properties", properties);
        ArrayNode required = objectMapper.createArrayNode();
        for (String property : properties.propertyNames()) {
            required.add(property);
        }
        value.set("required", required);
        return value;
    }

    private ObjectNode stringArray() {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", "array");
        value.set("items", stringType());
        return value;
    }

    private ObjectNode stringType() {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", "string");
        value.put("minLength", 1);
        return value;
    }

    private ObjectNode integerType() {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", "integer");
        return value;
    }

    private ObjectNode nullableInteger(Integer minimum, Integer maximum) {
        ObjectNode value = objectMapper.createObjectNode();
        ArrayNode types = objectMapper.createArrayNode();
        types.add("integer");
        types.add("null");
        value.set("type", types);
        if (minimum != null) {
            value.put("minimum", minimum);
        }
        if (maximum != null) {
            value.put("maximum", maximum);
        }
        return value;
    }
}
