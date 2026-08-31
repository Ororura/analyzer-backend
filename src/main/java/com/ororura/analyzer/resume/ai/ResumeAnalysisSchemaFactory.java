package com.ororura.analyzer.resume.ai;

import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
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

    public JsonNode create(MarketAnalysisProfile profile) {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("assessments", assessments(profile));
        properties.set("experienceAssessment", scoredObject("commercialRelevance", "experienceDescriptionQuality",
                "responsibilityLevel"));
        properties.set("resumeAssessment", scoredObject("resumeQuality", "atsReadability"));
        properties.set("skills", skills(profile));
        properties.set("skillEvidence", skillEvidence(profile));
        properties.set("atsFields", atsFields());
        properties.set("claims", claims());
        properties.set("interviewTopics", interviewTopics());
        properties.set("employmentPeriods", employmentPeriods());
        properties.set("strengths", stringArray());
        properties.set("weaknesses", stringArray());
        properties.set("atsIssues", stringArray());
        properties.set("recommendations", stringArray());
        properties.set("warnings", stringArray());
        return closedRequiredObject(properties);
    }

    private ObjectNode skillEvidence(MarketAnalysisProfile profile) {
        ObjectNode item = objectMapper.createObjectNode();
        item.set("skill", requirementEnum(profile, false));
        ObjectNode evidenceItem = objectMapper.createObjectNode();
        evidenceItem.set("text", stringType());
        evidenceItem.set("context", enumType("COMMERCIAL_TASK", "PROJECT_TASK", "STACK_CONTEXT", "SKILLS_SECTION"));
        evidenceItem.set("concrete", booleanType());
        evidenceItem.set("hasOutcome", booleanType());
        item.set("evidence", array(closedRequiredObject(evidenceItem), null, 10));
        return array(closedRequiredObject(item), null, profile.requirements().size());
    }

    private ObjectNode atsFields() {
        ObjectNode item = objectMapper.createObjectNode();
        item.set("field", enumType("parsing", "sections", "contacts", "experience", "education", "skills"));
        item.set("status", enumType("OK", "PARTIAL", "FAILED"));
        item.set("issues", stringArray());
        return array(closedRequiredObject(item), 6, 6);
    }

    private ObjectNode claims() {
        ObjectNode item = objectMapper.createObjectNode();
        item.set("claim", stringType());
        item.set("valueScore", boundedInteger(0, 10));
        item.set("credibilityScore", boundedInteger(0, 10));
        item.set("interviewRisk", boundedInteger(0, 10));
        item.set("evidenceQuality", boundedInteger(0, 10));
        item.set("explanation", stringType());
        return array(closedRequiredObject(item), null, 20);
    }

    private ObjectNode interviewTopics() {
        ObjectNode item = objectMapper.createObjectNode();
        item.set("topic", stringType());
        item.set("sourceClaim", stringType());
        item.set("questions", array(stringType(), null, 3));
        return array(closedRequiredObject(item), null, 15);
    }

    private ObjectNode assessments(MarketAnalysisProfile profile) {
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode criterion = stringType();
        ArrayNode allowed = objectMapper.createArrayNode();
        profile.criteria().forEach(value -> allowed.add(value.id()));
        criterion.set("enum", allowed);
        properties.set("criterionId", criterion);
        ObjectNode score = integerType();
        score.put("minimum", 0);
        score.put("maximum", 10);
        properties.set("score", score);
        properties.set("evidence", stringArray());
        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", "array");
        result.put("minItems", profile.criteria().size());
        result.put("maxItems", profile.criteria().size());
        result.set("items", closedRequiredObject(properties));
        return result;
    }

    private ObjectNode skills(MarketAnalysisProfile profile) {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("confirmed", requirementArray(profile, false));
        properties.set("weakEvidence", requirementArray(profile, false));
        properties.set("missing", requirementArray(profile, true));
        return closedRequiredObject(properties);
    }

    private ObjectNode requirementArray(MarketAnalysisProfile profile, boolean positiveFrequencyOnly) {
        ObjectNode item = requirementEnum(profile, positiveFrequencyOnly);
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", "array");
        value.set("items", item);
        return value;
    }

    private ObjectNode requirementEnum(MarketAnalysisProfile profile, boolean positiveFrequencyOnly) {
        ObjectNode item = stringType();
        ArrayNode allowed = objectMapper.createArrayNode();
        profile.requirements().stream()
                .filter(value -> !positiveFrequencyOnly || value.frequency() > 0)
                .forEach(value -> allowed.add(value.label()));
        item.set("enum", allowed);
        return item;
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

    private ObjectNode boundedInteger(int minimum, int maximum) {
        ObjectNode value = integerType(); value.put("minimum", minimum); value.put("maximum", maximum); return value;
    }

    private ObjectNode booleanType() {
        ObjectNode value = objectMapper.createObjectNode(); value.put("type", "boolean"); return value;
    }

    private ObjectNode enumType(String... values) {
        ObjectNode value = stringType(); ArrayNode allowed = objectMapper.createArrayNode();
        java.util.Arrays.stream(values).forEach(allowed::add); value.set("enum", allowed); return value;
    }

    private ObjectNode array(ObjectNode items, Integer minimum, Integer maximum) {
        ObjectNode value = objectMapper.createObjectNode(); value.put("type", "array"); value.set("items", items);
        if (minimum != null) value.put("minItems", minimum);
        if (maximum != null) value.put("maxItems", maximum);
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
