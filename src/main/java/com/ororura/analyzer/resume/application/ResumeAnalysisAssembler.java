package com.ororura.analyzer.resume.application;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.InterviewChance;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.domain.ExperienceModels.ExperienceSummary;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketProfileSource;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class ResumeAnalysisAssembler {

    private final ResumeAnalysisProperties resumeProperties;
    private final ResumeAnalysisMarkdownRenderer markdownRenderer;

    @Autowired
    public ResumeAnalysisAssembler(ResumeAnalysisProperties resumeProperties,
            ResumeAnalysisMarkdownRenderer markdownRenderer) {
        this.resumeProperties = resumeProperties;
        this.markdownRenderer = markdownRenderer;
    }

    public ResumeAnalysisAssembler(ResumeAnalysisProperties resumeProperties) {
        this(resumeProperties, new ResumeAnalysisMarkdownRenderer());
    }

    public ResumeAnalysisResult assemble(MarketAnalysisProfile profile, LlmResumeAnalysisResponse llm,
            ExperienceSummary experience, TechnologyProfile technologies, VacancyMarketData market,
            int commercialScore, int ats, int overall, int candidateStrength, CandidateLevel level,
            InterviewChance hrChance, InterviewChance technicalChance, Instant generatedAt,
            AiProviderType provider, String model, MarketProfileSource profileSource,
            DeterministicAnalysisEngine.AnalysisDetails details) {
        LlmResumeAnalysisResponse.ExperienceAssessment experienceAssessment = llm.experienceAssessment();
        ResumeAnalysisResult.Scores scores = new ResumeAnalysisResult.Scores(
                llm.assessments(), commercialScore,
                experienceAssessment.experienceDescriptionQuality().score(), ats,
                llm.resumeAssessment().resumeQuality().score() * 10);
        List<String> legacyWeaknesses = details.risks().stream().map(com.ororura.analyzer.resume.api.ResumeRisk::title)
                .distinct().toList();
        List<String> legacyAtsIssues = details.risks().stream()
                .filter(value -> value.type() == com.ororura.analyzer.resume.api.ResumeRisk.RiskType.ATS)
                .map(com.ororura.analyzer.resume.api.ResumeRisk::title).distinct().toList();
        List<String> legacyRecommendations = details.recommendations().items().stream()
                .map(com.ororura.analyzer.resume.api.ResumeRecommendationAnalysis.Recommendation::title).toList();
        ResumeAnalysisResult result = new ResumeAnalysisResult(profile.targetRole(), level, scores, overall, candidateStrength,
                hrChance, technicalChance,
                new ResumeAnalysisResult.Experience(experience.commercialMonths(), experience.commercialYears(),
                        experience.remainingMonths()),
                new ResumeAnalysisResult.Skills(technologies.confirmed(), technologies.weakEvidence(),
                        technologies.missing()),
                llm.strengths(), legacyWeaknesses, legacyAtsIssues, legacyRecommendations, details.vacancyFit(),
                new ResumeAnalysisResult.Market(market.source(), market.sampleSize()),
                new ResumeAnalysisResult.Metadata(profile.profile(), resumeProperties.analysisVersion(),
                        resumeProperties.baselineVersion(),
                        profile.version(), profileSource, generatedAt, provider, model),
                details.marketFit(), details.marketPosition(), details.technicalProfile(), details.skillEvidence(),
                details.skillGaps(), details.skillRoi(), details.gradeFit(), details.ats(), details.claimRisks(),
                details.interviewRisks(), details.risks(), details.recommendations(), null,
                warnings(market.warnings(), llm.warnings()));
        return result.withMarkdownReport(markdownRenderer.render(result));
    }

    private static List<String> warnings(List<String> market, List<String> llm) {
        LinkedHashSet<String> result = new LinkedHashSet<>(market);
        result.addAll(llm);
        return List.copyOf(result);
    }
}
