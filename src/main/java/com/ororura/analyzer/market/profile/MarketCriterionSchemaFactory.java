package com.ororura.analyzer.market.profile;

import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.requirement.*;
import com.ororura.analyzer.market.application.port.*;

import java.util.List;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
class MarketCriterionSchemaFactory {

    private final ObjectMapper objectMapper;
    private final MarketCriterionProperties properties;

    MarketCriterionSchemaFactory(ObjectMapper objectMapper, MarketCriterionProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    JsonNode create(List<String> requirementIds) {
        ObjectNode criterionProperties = objectMapper.createObjectNode();
        criterionProperties.set("label", stringType());
        criterionProperties.set("description", stringType());
        criterionProperties.set("requirementIds", requirementIds(requirementIds));
        ObjectNode rootProperties = objectMapper.createObjectNode();
        rootProperties.set("criteria", arrayOf(closedRequiredObject(criterionProperties),
                properties.minimumCount(), properties.maximumCount()));
        return closedRequiredObject(rootProperties);
    }

    private ObjectNode requirementIds(List<String> ids) {
        ObjectNode item = stringType();
        ArrayNode allowed = objectMapper.createArrayNode();
        ids.forEach(allowed::add);
        item.set("enum", allowed);
        return arrayOf(item, 1, null);
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
