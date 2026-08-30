package com.ororura.analyzer.vacancy.requirement;

import java.util.List;

import com.ororura.analyzer.resume.market.RequirementType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
class VacancyRequirementSchemaFactory {

    private final ObjectMapper objectMapper;

    VacancyRequirementSchemaFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    JsonNode create(List<VacancyRequirementInput> vacancies) {
        ObjectNode requirementProperties = objectMapper.createObjectNode();
        requirementProperties.set("label", stringType());
        requirementProperties.set("type", enumType(java.util.Arrays.stream(RequirementType.values())
                .map(Enum::name).toList()));
        requirementProperties.set("importance", enumType(java.util.Arrays.stream(RequirementImportance.values())
                .map(Enum::name).toList()));

        ObjectNode vacancyProperties = objectMapper.createObjectNode();
        vacancyProperties.set("vacancyId", enumType(vacancies.stream()
                .map(VacancyRequirementInput::vacancyId).toList()));
        vacancyProperties.set("requirements", arrayOf(closedRequiredObject(requirementProperties), 0, null));

        ObjectNode rootProperties = objectMapper.createObjectNode();
        rootProperties.set("vacancies", arrayOf(closedRequiredObject(vacancyProperties),
                vacancies.size(), vacancies.size()));
        return closedRequiredObject(rootProperties);
    }

    private ObjectNode enumType(List<String> values) {
        ObjectNode type = stringType();
        ArrayNode allowed = objectMapper.createArrayNode();
        values.forEach(allowed::add);
        type.set("enum", allowed);
        return type;
    }

    private ObjectNode arrayOf(JsonNode items, Integer minimum, Integer maximum) {
        ObjectNode array = objectMapper.createObjectNode();
        array.put("type", "array");
        array.set("items", items);
        if (minimum != null) array.put("minItems", minimum);
        if (maximum != null) array.put("maxItems", maximum);
        return array;
    }

    private ObjectNode closedRequiredObject(ObjectNode properties) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", "object");
        value.put("additionalProperties", false);
        value.set("properties", properties);
        ArrayNode required = objectMapper.createArrayNode();
        properties.propertyNames().forEach(required::add);
        value.set("required", required);
        return value;
    }

    private ObjectNode stringType() {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", "string");
        value.put("minLength", 1);
        return value;
    }
}
