package com.ororura.analyzer.ats;

import java.util.List;

import com.ororura.analyzer.vacancy.market.VacancyMarketAggregator;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;

import static com.ororura.analyzer.ats.AtsModels.*;

final class AtsFixture {

    private AtsFixture() {
    }

    static RawAtsAnalysis raw() {
        FilterAssessment unknown = new FilterAssessment(FilterStatus.UNKNOWN, null);
        return new RawAtsAnalysis(
                82,
                76,
                DetectedLevel.JUNIOR_PLUS,
                79,
                new Experience(
                        experience("1 год 6 месяцев", "Ноябрь 2024 — Май 2026"),
                        experience("6 месяцев", "Ноябрь 2025 — Май 2026, Java и Spring Boot"),
                        experience("1 год 6 месяцев", "Ноябрь 2024 — Май 2026, backend-разработка"),
                        experience("1 год 6 месяцев", "Ноябрь 2024 — Май 2026, опыт работы в компаниях"),
                        experience(null, null)),
                new StructuredFilters(
                        new FilterAssessment(FilterStatus.MATCH, "Ноябрь 2024 — Май 2026, backend-разработка"),
                        unknown, unknown, unknown, unknown, unknown, unknown, unknown),
                List.of(
                        technology("Java", TechnologyStatus.CONFIRMED_EXPERIENCE, "6 месяцев Java и Spring Boot"),
                        technology("Spring Boot", TechnologyStatus.SEMANTIC_EXPERIENCE, "REST endpoints на Spring Boot"),
                        technology("REST API", TechnologyStatus.SEMANTIC_EXPERIENCE, "REST endpoints на Spring Boot"),
                        technology("PostgreSQL", TechnologyStatus.SKILLS_ONLY, "Skills: PostgreSQL"),
                        technology("Kafka", TechnologyStatus.MISSING, null),
                        technology("Kubernetes", TechnologyStatus.MISSING, null)),
                new ScoreEvidence(
                        List.of("Заголовок Java Backend Developer"),
                        List.of("Специализация указана в заголовке"),
                        List.of("Есть отдельный backend и Java опыт")),
                List.of("Java/Spring явно подтверждены"),
                List.of("PostgreSQL указан только в Skills"),
                List.of("Не указан формат работы"),
                List.of(new Recommendation(
                        "high", "Опыт", "PostgreSQL не подтверждён опытом",
                        "Если у тебя действительно есть опыт с PostgreSQL, покажи его в описании работы или проекта.",
                        null, "Skills: PostgreSQL")),
                "Релевантный Junior+ Java Backend кандидат.");
    }

    static VacancyMarketData baselineMarket() {
        return new VacancyMarketAggregator().baseline(null);
    }

    static RawTechnologyAssessment technology(String name, TechnologyStatus status, String evidence) {
        return new RawTechnologyAssessment(name, status, evidence);
    }

    static ExperienceAssessment experience(String value, String evidence) {
        return new ExperienceAssessment(value, evidence);
    }

    static RawAtsAnalysis withTechnologies(RawAtsAnalysis raw, List<RawTechnologyAssessment> technologies) {
        return new RawAtsAnalysis(
                raw.hhSearchMatch(), raw.recruiterReadability(), raw.detectedLevel(), raw.targetLevelFit(),
                raw.experience(), raw.structuredFilters(), technologies, raw.scoreEvidence(), raw.strengths(),
                raw.weaknesses(), raw.recruiterRisks(), raw.recommendations(), raw.summary());
    }
}
