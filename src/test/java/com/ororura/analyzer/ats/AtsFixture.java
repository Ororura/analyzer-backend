package com.ororura.analyzer.ats;

import java.util.List;

import com.ororura.analyzer.ats.domain.MatchingAssessment.TechnologyStatus;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis;
import com.ororura.analyzer.ats.domain.NormalizedAtsAnalysis.NormalizedTechnologyAssessment;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmExperience;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmExperienceAssessment;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmFilterAssessment;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmRecommendation;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmScoreEvidence;
import com.ororura.analyzer.ats.llm.dto.LlmAtsAnalysis.LlmStructuredFilters;
import com.ororura.analyzer.ats.llm.dto.LlmTechnologyAssessment;
import com.ororura.analyzer.ats.normalization.AtsAnalysisNormalizer;
import com.ororura.analyzer.vacancy.market.VacancyMarketAggregator;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;

final class AtsFixture {

    private AtsFixture() {
    }

    static LlmAtsAnalysis llm() {
        LlmFilterAssessment unknown = new LlmFilterAssessment("unknown", null);
        return new LlmAtsAnalysis(
                82,
                76,
                "junior_plus",
                79,
                new LlmExperience(
                        experience("1 год 6 месяцев", "Ноябрь 2024 — Май 2026"),
                        experience("6 месяцев", "Ноябрь 2025 — Май 2026, Java и Spring Boot"),
                        experience("1 год 6 месяцев", "Ноябрь 2024 — Май 2026, backend-разработка"),
                        experience("1 год 6 месяцев", "Ноябрь 2024 — Май 2026, опыт работы в компаниях"),
                        experience(null, null)),
                new LlmStructuredFilters(
                        new LlmFilterAssessment("match", "Ноябрь 2024 — Май 2026, backend-разработка"),
                        unknown, unknown, unknown, unknown, unknown, unknown, unknown),
                List.of(
                        technology("Java", "confirmed_experience", "6 месяцев Java и Spring Boot"),
                        technology("Spring Boot", "semantic_experience", "REST endpoints на Spring Boot"),
                        technology("REST API", "semantic_experience", "REST endpoints на Spring Boot"),
                        technology("PostgreSQL", "skills_only", "Skills: PostgreSQL"),
                        technology("Kafka", "missing", null),
                        technology("Kubernetes", "missing", null)),
                new LlmScoreEvidence(
                        List.of("Заголовок Java Backend Developer"),
                        List.of("Специализация указана в заголовке"),
                        List.of("Есть отдельный backend и Java опыт")),
                List.of("Java/Spring явно подтверждены"),
                List.of("PostgreSQL указан только в Skills"),
                List.of("Не указан формат работы"),
                List.of(new LlmRecommendation(
                        "high", "Опыт", "PostgreSQL не подтверждён опытом",
                        "Если у тебя действительно есть опыт с PostgreSQL, покажи его в описании работы или проекта.",
                        null, "Skills: PostgreSQL")),
                "Релевантный Junior+ Java Backend кандидат.");
    }

    static NormalizedAtsAnalysis normalized() {
        return new AtsAnalysisNormalizer().normalize(llm()).result();
    }

    static VacancyMarketData baselineMarket() {
        return new VacancyMarketAggregator().baseline(null);
    }

    static LlmTechnologyAssessment technology(String name, String status, String evidence) {
        return new LlmTechnologyAssessment(name, status, evidence);
    }

    static LlmExperienceAssessment experience(String value, String evidence) {
        return new LlmExperienceAssessment(value, evidence);
    }

    static LlmAtsAnalysis withTechnologies(LlmAtsAnalysis value, List<LlmTechnologyAssessment> technologies) {
        return new LlmAtsAnalysis(
                value.hhSearchMatch(), value.recruiterReadability(), value.detectedLevel(), value.targetLevelFit(),
                value.experience(), value.structuredFilters(), technologies, value.scoreEvidence(), value.strengths(),
                value.weaknesses(), value.recruiterRisks(), value.recommendations(), value.summary());
    }

    static NormalizedAtsAnalysis withTechnologies(
            NormalizedAtsAnalysis value,
            List<NormalizedTechnologyAssessment> technologies) {
        return new NormalizedAtsAnalysis(
                value.candidate(), value.filters(), technologies, value.hhSearchMatch(), value.recruiterReadability(),
                value.targetLevelFit(), value.insights());
    }

    static NormalizedTechnologyAssessment technology(
            String name,
            TechnologyStatus status,
            String evidence) {
        return new NormalizedTechnologyAssessment(name, status, evidence);
    }
}
