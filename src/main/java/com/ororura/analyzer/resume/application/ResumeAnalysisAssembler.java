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
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisAssembler {

    private final ResumeAnalysisProperties resumeProperties;

    public ResumeAnalysisAssembler(ResumeAnalysisProperties resumeProperties) {
        this.resumeProperties = resumeProperties;
    }

    public ResumeAnalysisResult assemble(LlmResumeAnalysisResponse llm, ExperienceSummary experience, TechnologyProfile technologies, VacancyMarketData market, int commercialScore, int ats, int overall, int candidateStrength, CandidateLevel level, InterviewChance hrChance, InterviewChance technicalChance, Instant generatedAt, AiProviderType provider, String model) {
        LlmResumeAnalysisResponse.TechnicalAssessment technical = llm.technicalAssessment();
        LlmResumeAnalysisResponse.ExperienceAssessment experienceAssessment = llm.experienceAssessment();
        ResumeAnalysisResult.Scores scores = new ResumeAnalysisResult.Scores(
                technical.javaDepth().score(), technical.springDepth().score(), technical.backendDepth().score(),
                technical.sqlPostgresqlDepth().score(), technical.hibernateJpaDepth().score(),
                technical.infrastructureDepth().score(), technical.messagingCacheDepth().score(),
                technical.testingDepth().score(), commercialScore,
                experienceAssessment.experienceDescriptionQuality().score(), ats,
                llm.resumeAssessment().resumeQuality().score() * 10);
        return new ResumeAnalysisResult(resumeProperties.targetRole(), level, scores, overall, candidateStrength, hrChance, technicalChance, new ResumeAnalysisResult.Experience(experience.commercialMonths(), experience.commercialYears(), experience.remainingMonths()), new ResumeAnalysisResult.Skills(technologies.confirmed(), technologies.weakEvidence(), technologies.missing()), llm.strengths(), llm.weaknesses(), llm.atsIssues(), llm.recommendations(), new ResumeAnalysisResult.Market(market.source(), market.sampleSize()), new ResumeAnalysisResult.Metadata(resumeProperties.analysisVersion(), resumeProperties.baselineVersion(), generatedAt, provider, model), warnings(market.warnings(), llm.warnings()));
    }

    private static List<String> warnings(List<String> market, List<String> llm) {
        LinkedHashSet<String> result = new LinkedHashSet<>(market);
        result.addAll(llm);
        return List.copyOf(result);
    }
}
