package com.ororura.analyzer.resume.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.api.*;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;
import com.ororura.analyzer.resume.domain.*;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class DeterministicAnalysisEngine {
    private final EvidenceClassifier evidenceClassifier;
    private final TechnicalProfileScorer technicalScorer;
    private final GradeFitScorer gradeScorer;
    private final MarketFitScorer marketFitScorer;
    private final SkillGapScorer gapScorer;
    private final SkillRoiCalculator roiCalculator;
    private final AtsScoreCalculator atsCalculator;
    private final ClaimRiskAnalyzer claimAnalyzer;
    private final VacancyFitScorer vacancyScorer;
    private final RiskDeduplicator riskDeduplicator;
    private final RecommendationRanker recommendationRanker;
    private final MarketPercentileCalculator percentileCalculator;

    public DeterministicAnalysisEngine(EvidenceClassifier evidenceClassifier,
            TechnicalProfileScorer technicalScorer, GradeFitScorer gradeScorer,
            MarketFitScorer marketFitScorer, SkillGapScorer gapScorer, SkillRoiCalculator roiCalculator,
            AtsScoreCalculator atsCalculator, ClaimRiskAnalyzer claimAnalyzer, VacancyFitScorer vacancyScorer,
            RiskDeduplicator riskDeduplicator, RecommendationRanker recommendationRanker,
            MarketPercentileCalculator percentileCalculator) {
        this.evidenceClassifier = evidenceClassifier; this.technicalScorer = technicalScorer;
        this.gradeScorer = gradeScorer; this.marketFitScorer = marketFitScorer; this.gapScorer = gapScorer;
        this.roiCalculator = roiCalculator; this.atsCalculator = atsCalculator; this.claimAnalyzer = claimAnalyzer;
        this.vacancyScorer = vacancyScorer; this.riskDeduplicator = riskDeduplicator;
        this.recommendationRanker = recommendationRanker; this.percentileCalculator = percentileCalculator;
    }

    public AnalysisDetails analyze(MarketAnalysisProfile profile, LlmResumeAnalysisResponse llm, String text,
            VacancyMarketData market, int commercialScore, int commercialMonths,
            AtsScoreCalculator.AtsScore legacyAts, int technicalScore, CandidateLevel level, boolean singleVacancy) {
        return analyze(profile,llm,text,market,commercialScore,commercialMonths,legacyAts,technicalScore,level,singleVacancy,null);
    }

    public AnalysisDetails analyze(MarketAnalysisProfile profile, LlmResumeAnalysisResponse llm, String text,
            VacancyMarketData market, int commercialScore, int commercialMonths,
            AtsScoreCalculator.AtsScore legacyAts, int technicalScore, CandidateLevel level, boolean singleVacancy,
            com.ororura.analyzer.analysis.domain.CandidateGrade targetGrade) {
        var evidence = evidenceClassifier.classify(profile, llm);
        var technical = technicalScorer.score(llm.assessments(), profile.criteria());
        var grade = targetGrade == null ? gradeScorer.score(level, market) : gradeScorer.score(
                com.ororura.analyzer.analysis.domain.CandidateGrade.fromLegacy(level), targetGrade);
        var ats = atsCalculator.details(text, llm, legacyAts);
        var marketFit = marketFitScorer.score(profile, evidence, commercialScore, ats.score(), grade);
        var gaps = gapScorer.score(profile, evidence);
        var roi = roiCalculator.calculate(gaps);
        var claims = claimAnalyzer.claims(llm);
        var interviews = claimAnalyzer.interviews(llm);
        var vacancy = singleVacancy ? vacancyScorer.score(market, evidence, commercialMonths, grade, technicalScore) : null;
        var risks = riskDeduplicator.deduplicate(risks(llm, gaps, ats, claims, vacancy));
        var recommendations = recommendationRanker.build(ats, gaps, claims, llm.recommendations());
        return new AnalysisDetails(marketFit, percentileCalculator.calculate(), technical, evidence, gaps, roi,
                grade, ats, claims, interviews, risks, recommendations, vacancy);
    }

    private static List<ResumeRisk> risks(LlmResumeAnalysisResponse llm, SkillGapAnalysis gaps, AtsAnalysis ats,
            ResumeClaimRiskAnalysis claims, VacancyFitAnalysis vacancy) {
        List<ResumeRisk> risks = new ArrayList<>();
        gaps.gaps().stream().filter(value -> value.priority() != SkillGapAnalysis.GapPriority.LOW).forEach(value ->
                risks.add(new ResumeRisk(ResumeRisk.RiskType.SKILL_GAP,
                        value.priority() == SkillGapAnalysis.GapPriority.HIGH
                                ? ResumeRisk.RiskSeverity.HIGH : ResumeRisk.RiskSeverity.MEDIUM,
                        value.skillId(), "Недостаточно подтверждён навык " + value.skill(), value.candidateStatus().name(),
                        "Снижает покрытие требований рынка", "Добавить только реальный evidence или изучить навык")));
        ats.diagnostics().stream().filter(value -> value.status() != AtsAnalysis.FieldStatus.OK).forEach(value ->
                risks.add(new ResumeRisk(ResumeRisk.RiskType.ATS,
                        value.status() == AtsAnalysis.FieldStatus.FAILED
                                ? ResumeRisk.RiskSeverity.HIGH : ResumeRisk.RiskSeverity.MEDIUM,
                        value.field(), "ATS не полностью распознаёт " + value.field(), String.join("; ", value.issues()),
                        "Поле может не участвовать в поиске", "Уточнить структуру раздела")));
        claims.claims().stream().filter(value -> value.interviewRisk() >= 7).forEach(value ->
                risks.add(new ResumeRisk(ResumeRisk.RiskType.CLAIM, ResumeRisk.RiskSeverity.HIGH,
                        normalize(value.claim()), "Claim потребует доказательств", value.claim(),
                        "Высокая вероятность уточняющих вопросов", "Подготовить методику и исходные данные")));
        llm.weaknesses().forEach(value -> risks.add(new ResumeRisk(ResumeRisk.RiskType.DATA_QUALITY,
                ResumeRisk.RiskSeverity.MEDIUM, normalize(value), value, value,
                "Может снизить доверие к резюме", "Уточнить без добавления несуществующего опыта")));
        if (vacancy != null) vacancy.blockers().forEach(value -> risks.add(new ResumeRisk(
                ResumeRisk.RiskType.VACANCY_BLOCKER, ResumeRisk.RiskSeverity.HIGH,
                value.type().name(), "Обязательный blocker: " + value.type(), value.actual(), value.required(),
                "Не скрывать разрыв; выбирать вакансию осознанно")));
        return risks;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public record AnalysisDetails(MarketFitAnalysis marketFit, MarketPosition marketPosition,
            TechnicalProfile technicalProfile, SkillEvidenceAnalysis skillEvidence, SkillGapAnalysis skillGaps,
            SkillRoiAnalysis skillRoi, GradeFitAnalysis gradeFit, AtsAnalysis ats,
            ResumeClaimRiskAnalysis claimRisks, InterviewRiskAnalysis interviewRisks, List<ResumeRisk> risks,
            ResumeRecommendationAnalysis recommendations, VacancyFitAnalysis vacancyFit) {
    }
}
