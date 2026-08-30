package com.ororura.analyzer.vacancy.requirement;

import java.util.List;
import java.util.HashSet;

import org.springframework.stereotype.Component;

@Component
public class VacancyRequirementExtractor {

    private final VacancyRequirementPromptFactory promptFactory;
    private final VacancyRequirementAiGateway aiGateway;
    private final VacancyRequirementResponseParser responseParser;

    VacancyRequirementExtractor(VacancyRequirementPromptFactory promptFactory,
            VacancyRequirementAiGateway aiGateway, VacancyRequirementResponseParser responseParser) {
        this.promptFactory = promptFactory;
        this.aiGateway = aiGateway;
        this.responseParser = responseParser;
    }

    public List<ExtractedVacancyRequirements> extract(List<VacancyRequirementInput> vacancies) {
        if (vacancies == null || vacancies.isEmpty()) {
            return List.of();
        }
        List<VacancyRequirementInput> immutable = List.copyOf(vacancies);
        HashSet<String> ids = new HashSet<>();
        if (immutable.stream().map(VacancyRequirementInput::vacancyId).anyMatch(id -> !ids.add(id))) {
            throw new IllegalArgumentException("Vacancy extraction batch contains duplicate vacancy ids");
        }
        VacancyRequirementPrompt prompt = promptFactory.create(immutable);
        String response = aiGateway.complete(prompt);
        return responseParser.parse(response, immutable.stream()
                .map(VacancyRequirementInput::vacancyId).toList());
    }
}
