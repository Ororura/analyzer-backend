package com.ororura.analyzer.resume.application;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.ai.LegacyResumeAnalysisProfileDefinition;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.CandidateLevel;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult.InterviewChance;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.domain.ExperienceModels.ExperienceSummary;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileFallback;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisAssembler {

    private final ResumeAnalysisProperties resumeProperties;
    private final MarketAnalysisProfileFallback marketProfileFallback;

    public ResumeAnalysisAssembler(ResumeAnalysisProperties resumeProperties,
            MarketAnalysisProfileFallback marketProfileFallback) {
        this.resumeProperties = resumeProperties;
        this.marketProfileFallback = marketProfileFallback;
    }

    public ResumeAnalysisResult assemble(LegacyResumeAnalysisProfileDefinition profile, LlmResumeAnalysisResponse llm,
            ExperienceSummary experience, TechnologyProfile technologies, VacancyMarketData market,
            int commercialScore, int ats, int overall, int candidateStrength, CandidateLevel level,
            InterviewChance hrChance, InterviewChance technicalChance, Instant generatedAt,
            AiProviderType provider, String model) {
        LlmResumeAnalysisResponse.ExperienceAssessment experienceAssessment = llm.experienceAssessment();
        ResumeAnalysisResult.Scores scores = new ResumeAnalysisResult.Scores(
                llm.semanticScores(), commercialScore,
                experienceAssessment.experienceDescriptionQuality().score(), ats,
                llm.resumeAssessment().resumeQuality().score() * 10);
        return new ResumeAnalysisResult(profile.targetRole(), level, scores, overall, candidateStrength,
                hrChance, technicalChance,
                new ResumeAnalysisResult.Experience(experience.commercialMonths(), experience.commercialYears(),
                        experience.remainingMonths()),
                new ResumeAnalysisResult.Skills(technologies.confirmed(), technologies.weakEvidence(),
                        technologies.missing()),
                llm.strengths(), llm.weaknesses(), llm.atsIssues(), llm.recommendations(),
                vacancyFit(llm, technologies, market, level),
                new ResumeAnalysisResult.Market(market.source(), market.sampleSize()),
                new ResumeAnalysisResult.Metadata(resumeProperties.analysisVersion(),
                        resumeProperties.baselineVersion(),
                        marketProfileFallback.get(profile.profile()).version(), generatedAt, provider, model),
                warnings(market.warnings(), llm.warnings()));
    }

    private static ResumeAnalysisResult.VacancyFit vacancyFit(LlmResumeAnalysisResponse llm,
            TechnologyProfile technologies, VacancyMarketData market, CandidateLevel level) {
        List<String> required = market.skillFrequencies().entrySet().stream()
                .filter(entry -> entry.getValue() >= 0.5).map(Map.Entry::getKey).toList();
        List<String> optional = market.skillFrequencies().entrySet().stream()
                .filter(entry -> entry.getValue() > 0 && entry.getValue() < 0.5).map(Map.Entry::getKey).toList();
        LinkedHashSet<String> rejectionReasons = new LinkedHashSet<>(llm.weaknesses());
        rejectionReasons.addAll(llm.atsIssues());
        return new ResumeAnalysisResult.VacancyFit(required, optional, technologies.missing(),
                llm.experienceAssessment().commercialRelevance().score(), levelFit(level, market),
                llm.weaknesses(), List.copyOf(rejectionReasons));
    }

    private static String levelFit(CandidateLevel level, VacancyMarketData market) {
        if (market.experienceRequirements().isEmpty()) return "UNKNOWN";
        String requirements = String.join(" ", market.experienceRequirements().keySet()).toLowerCase();
        boolean matches = switch (level) {
            case JUNIOR, JUNIOR_PLUS -> requirements.contains("нет опыта") || requirements.contains("1–3")
                    || requirements.contains("1-3");
            case MIDDLE_MINUS, MIDDLE -> requirements.contains("1–3") || requirements.contains("1-3")
                    || requirements.contains("3–6") || requirements.contains("3-6");
        };
        return matches ? "MATCH" : "PARTIAL";
    }

    private static List<String> warnings(List<String> market, List<String> llm) {
        LinkedHashSet<String> result = new LinkedHashSet<>(market);
        result.addAll(llm);
        return List.copyOf(result);
    }
}
