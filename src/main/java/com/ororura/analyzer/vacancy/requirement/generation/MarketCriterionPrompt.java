package com.ororura.analyzer.vacancy.requirement.generation;

import tools.jackson.databind.JsonNode;

record MarketCriterionPrompt(String systemInstruction, JsonNode input, JsonNode schema) {
}
