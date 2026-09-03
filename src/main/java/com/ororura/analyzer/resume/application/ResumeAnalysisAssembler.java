package com.ororura.analyzer.resume.application;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import com.ororura.analyzer.resume.application.port.AiProviderType;
import com.ororura.analyzer.analysis.semantic.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.domain.analysis.CandidateLevel;
import com.ororura.analyzer.resume.domain.analysis.InterviewChance;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.domain.ExperienceModels.ExperienceSummary;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.domain.MarketProfileSource;
import com.ororura.analyzer.market.domain.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisAssembler {

    private final ResumeAnalysisProperties resumeProperties;
    public ResumeAnalysisAssembler(ResumeAnalysisProperties resumeProperties) {
        this.resumeProperties = resumeProperties;
    }

    public ResumeAnalysisOutcome assemble(MarketAnalysisProfile profile, LlmResumeAnalysisResponse llm,
            ExperienceSummary experience, TechnologyProfile technologies, VacancyMarketData market,
            int commercialScore, int ats, int overall, int candidateStrength, CandidateLevel level,
            InterviewChance hrChance, InterviewChance technicalChance, Instant generatedAt,
            AiProviderType provider, String model, MarketProfileSource profileSource,
            DeterministicAnalysisEngine.AnalysisDetails details) {
        LlmResumeAnalysisResponse.ExperienceAssessment experienceAssessment = llm.experienceAssessment();
        ResumeAnalysisOutcome.Scores scores = new ResumeAnalysisOutcome.Scores(
                llm.assessments(), commercialScore,
                experienceAssessment.experienceDescriptionQuality().score(), ats,
                llm.resumeAssessment().resumeQuality().score() * 10);
        List<String> legacyWeaknesses = details.risks().stream().map(com.ororura.analyzer.resume.domain.analysis.ResumeRisk::title)
                .distinct().toList();
        List<String> legacyAtsIssues = details.risks().stream()
                .filter(value -> value.type() == com.ororura.analyzer.resume.domain.analysis.ResumeRisk.RiskType.ATS)
                .map(com.ororura.analyzer.resume.domain.analysis.ResumeRisk::title).distinct().toList();
        List<String> legacyRecommendations = details.recommendations().items().stream()
                .map(com.ororura.analyzer.resume.domain.analysis.ResumeRecommendationAnalysis.Recommendation::title).toList();
        return new ResumeAnalysisOutcome(profile.targetRole(), level, scores, overall, candidateStrength,
                hrChance, technicalChance,
                new ResumeAnalysisOutcome.Experience(experience.commercialMonths(), experience.commercialYears(),
                        experience.remainingMonths()),
                new ResumeAnalysisOutcome.Skills(technologies.confirmed(), technologies.weakEvidence(),
                        technologies.missing()),
                llm.strengths(), legacyWeaknesses, legacyAtsIssues, legacyRecommendations, details.vacancyFit(),
                new ResumeAnalysisOutcome.Market(market.source(), market.sampleSize()),
                new ResumeAnalysisOutcome.Metadata(2, profile.profile(), resumeProperties.analysisVersion(),
                        resumeProperties.baselineVersion(), profile.version(), profileSource, generatedAt, provider, model),
                details.marketFit(), details.marketPosition(), details.technicalProfile(), details.skillEvidence(),
                details.skillGaps(), details.skillRoi(), details.gradeFit(), details.ats(), details.claimRisks(),
                details.interviewRisks(), details.risks(), details.recommendations(),
                warnings(market.warnings(), llm.warnings()));
    }

    private static List<String> warnings(List<String> market, List<String> llm) {
        LinkedHashSet<String> result = new LinkedHashSet<>(market);
        result.addAll(llm);
        return List.copyOf(result);
    }
}
