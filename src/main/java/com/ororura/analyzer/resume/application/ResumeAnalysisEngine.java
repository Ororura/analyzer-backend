package com.ororura.analyzer.resume.application;

import java.util.List;

import com.ororura.analyzer.market.domain.MarketAnalysisProfile;
import com.ororura.analyzer.market.domain.VacancyMarketData;
import com.ororura.analyzer.analysis.semantic.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.domain.AtsScoreCalculator;
import com.ororura.analyzer.resume.domain.CandidateLevelPolicy;
import com.ororura.analyzer.resume.domain.CandidateStrengthCalculator;
import com.ororura.analyzer.resume.domain.ExperienceCalculator;
import com.ororura.analyzer.resume.domain.ExperienceModels.EmploymentPeriod;
import com.ororura.analyzer.resume.domain.ExperienceModels.ExperienceSummary;
import com.ororura.analyzer.resume.domain.InterviewChancePolicy;
import com.ororura.analyzer.resume.domain.OverallScoreCalculator;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy;
import com.ororura.analyzer.resume.domain.TechnologyTaxonomy.TechnologyProfile;
import com.ororura.analyzer.resume.domain.analysis.CandidateLevel;
import com.ororura.analyzer.resume.domain.analysis.InterviewChance;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisEngine {

    private final ExperienceCalculator experienceCalculator;
    private final TechnologyTaxonomy taxonomy;
    private final AtsScoreCalculator atsCalculator;
    private final OverallScoreCalculator overallCalculator;
    private final CandidateStrengthCalculator strengthCalculator;
    private final CandidateLevelPolicy levelPolicy;
    private final InterviewChancePolicy chancePolicy;
    private final DeterministicAnalysisEngine detailsEngine;

    public ResumeAnalysisEngine(ExperienceCalculator experienceCalculator, TechnologyTaxonomy taxonomy,
            AtsScoreCalculator atsCalculator, OverallScoreCalculator overallCalculator,
            CandidateStrengthCalculator strengthCalculator, CandidateLevelPolicy levelPolicy,
            InterviewChancePolicy chancePolicy, DeterministicAnalysisEngine detailsEngine) {
        this.experienceCalculator = experienceCalculator;
        this.taxonomy = taxonomy;
        this.atsCalculator = atsCalculator;
        this.overallCalculator = overallCalculator;
        this.strengthCalculator = strengthCalculator;
        this.levelPolicy = levelPolicy;
        this.chancePolicy = chancePolicy;
        this.detailsEngine = detailsEngine;
    }

    public Result analyze(MarketAnalysisProfile profile, LlmResumeAnalysisResponse semanticAnalysis,
            List<EmploymentPeriod> periods, String resumeText, VacancyMarketData market, boolean singleVacancy) {
        ExperienceSummary experience = experienceCalculator.calculate(periods);
        int commercialScore = experienceCalculator.commercialScore(experience.commercialMonths());
        TechnologyProfile technologies = taxonomy.profile(profile, resumeText,
                semanticAnalysis.skills().confirmed(), semanticAnalysis.skills().weakEvidence(),
                semanticAnalysis.skills().missing());
        var ats = atsCalculator.calculate(semanticAnalysis.resumeAssessment().atsReadability().score(),
                semanticAnalysis.resumeAssessment().resumeQuality().score(),
                semanticAnalysis.experienceAssessment().experienceDescriptionQuality().score(),
                commercialScore, technologies, profile);
        int technicalScore = overallCalculator.technicalScore(semanticAnalysis.assessments(), profile.criteria());
        int overall = overallCalculator.overall(technicalScore, ats.total(), commercialScore,
                semanticAnalysis.resumeAssessment().resumeQuality().score() * 10,
                semanticAnalysis.experienceAssessment().responsibilityLevel().score());
        int strength = strengthCalculator.calculate(technicalScore, commercialScore,
                semanticAnalysis.experienceAssessment().responsibilityLevel().score());
        CandidateLevel level = levelPolicy.detect(experience.commercialMonths(), technicalScore,
                semanticAnalysis.experienceAssessment().responsibilityLevel().score(), overall);
        InterviewChance hrChance = chancePolicy.hr(ats.total(), overall);
        InterviewChance technicalChance = chancePolicy.technical(technicalScore, overall);
        var details = detailsEngine.analyze(profile, semanticAnalysis, resumeText, market, commercialScore,
                experience.commercialMonths(), ats, technicalScore, level, singleVacancy);
        return new Result(experience, technologies, commercialScore, ats.total(), overall, strength,
                level, hrChance, technicalChance, details);
    }

    public record Result(ExperienceSummary experience, TechnologyProfile technologies, int commercialScore,
            int atsScore, int overallScore, int candidateStrength, CandidateLevel level,
            InterviewChance hrChance, InterviewChance technicalChance,
            DeterministicAnalysisEngine.AnalysisDetails details) {
    }
}
