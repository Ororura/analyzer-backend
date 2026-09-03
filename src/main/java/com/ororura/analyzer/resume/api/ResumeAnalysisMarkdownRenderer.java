package com.ororura.analyzer.resume.api;

import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisMarkdownRenderer {
    public String render(ResumeAnalysisResult result) {
        StringBuilder out = new StringBuilder("# Анализ резюме\n\n## Итог\n\n");
        line(out, "Уровень", result.detectedLevel().value());
        line(out, "Общая оценка", result.overallScore());
        if (result.marketFit() != null && result.marketFit().score() != null) line(out, "Market Fit", result.marketFit().score());
        if (result.ats() != null) line(out, "ATS", result.ats().score());
        line(out, "Candidate Strength", result.candidateStrength());
        if (result.marketPosition() != null && result.marketPosition().juniorPercentile() != null) {
            section(out, "Позиция относительно рынка");
            line(out, "Junior percentile", result.marketPosition().juniorPercentile());
        }
        if (result.technicalProfile() != null) {
            section(out, "Технический профиль"); line(out, "Score", result.technicalProfile().score());
        }
        if (result.marketFit() != null) {
            section(out, "Покрытие рынка");
            line(out, "Must-have", result.marketFit().mustHaveCoverage());
            line(out, "Nice-to-have", result.marketFit().niceToHaveCoverage());
        }
        if (result.skillEvidence() != null && !result.skillEvidence().skills().isEmpty()) {
            section(out, "Подтверждение навыков");
            result.skillEvidence().skills().forEach(value -> out.append("- ").append(value.skill()).append(": ")
                    .append(value.status()).append("\n"));
        }
        if (result.skillGaps() != null && !result.skillGaps().gaps().isEmpty()) {
            section(out, "Skill gaps"); result.skillGaps().gaps().stream().limit(10).forEach(value ->
                    out.append("- ").append(value.skill()).append(": ").append(value.priority()).append("\n"));
        }
        if (result.skillRoi() != null && !result.skillRoi().skills().isEmpty()) {
            section(out, "Что выгоднее изучить"); result.skillRoi().skills().stream().limit(10).forEach(value ->
                    out.append("- ").append(value.skill()).append(": ROI ").append(value.roiScore()).append("\n"));
        }
        if (result.ats() != null) { section(out, "ATS"); line(out, "Score", result.ats().score()); }
        if (!result.risks().isEmpty()) {
            section(out, "Риски резюме"); result.risks().forEach(value -> out.append("- ")
                    .append(value.title()).append(": ").append(value.action()).append("\n"));
        }
        if (result.interviewRisks() != null && !result.interviewRisks().topics().isEmpty()) {
            section(out, "Риски на собеседовании"); result.interviewRisks().topics().forEach(value ->
                    out.append("- ").append(value.topic()).append(": ").append(value.risk()).append("\n"));
        }
        if (result.vacancyFit() != null) {
            section(out, "Соответствие вакансии"); line(out, "Score", result.vacancyFit().score());
            line(out, "Рекомендация", result.vacancyFit().applyRecommendation());
        }
        if (result.recommendationAnalysis() != null && !result.recommendationAnalysis().items().isEmpty()) {
            section(out, "Рекомендации"); result.recommendationAnalysis().items().forEach(value ->
                    out.append("- ").append(value.title()).append(" — ").append(value.reason()).append("\n"));
        }
        section(out, "Итоговая рекомендация");
        out.append(result.vacancyFit() == null ? "Сфокусируйтесь на рекомендациях с максимальным ожидаемым эффектом."
                : result.vacancyFit().applyRecommendation()).append("\n");
        return out.toString();
    }
    private static void section(StringBuilder out, String name) { out.append("\n## ").append(name).append("\n\n"); }
    private static void line(StringBuilder out, String name, Object value) {
        if (value != null) out.append(name).append(": ").append(value).append("\n");
    }
}
