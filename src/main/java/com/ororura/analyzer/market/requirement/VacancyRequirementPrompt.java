package com.ororura.analyzer.market.requirement;

import tools.jackson.databind.JsonNode;

record VacancyRequirementPrompt(String systemInstruction, JsonNode input, JsonNode schema) {
}
