package com.ororura.analyzer.vacancy.requirement;

import tools.jackson.databind.JsonNode;

record VacancyRequirementPrompt(String systemInstruction, JsonNode input, JsonNode schema) {
}
