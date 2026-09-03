package com.ororura.analyzer.resume.config;

import java.time.Clock;

import com.ororura.analyzer.resume.application.PdfFileValidator;
import com.ororura.analyzer.resume.domain.*;
import com.ororura.analyzer.analysis.profile.AnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.SkillNormalizer;
import com.ororura.analyzer.analysis.profile.DefaultAnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileDefinition;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.analysis.profile.definition.JavaBackendAnalysisProfile;
import com.ororura.analyzer.analysis.profile.definition.ReactFrontendAnalysisProfile;
import java.util.List;
import com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy;
import com.ororura.analyzer.integration.codex.CodexCliProperties;
import com.ororura.analyzer.integration.polza.PolzaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ResumeAnalysisProperties.class, ResumeScoringProperties.class, AiProperties.class,
        CodexCliProperties.class, PolzaProperties.class})
public class ResumeConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    PdfFileValidator pdfFileValidator(ResumeAnalysisProperties properties) {
        return new PdfFileValidator(properties);
    }

    @Bean
    AnalysisScoringPolicy resumeScoringPolicy(ResumeScoringProperties properties) {
        return properties.toPolicy();
    }

    @Bean
    AnalysisTechnologyCatalog analysisTechnologyCatalog() {
        return new DefaultAnalysisTechnologyCatalog();
    }

    @Bean
    ResumeAnalysisProfileDefinition javaBackendAnalysisProfile(ResumeAnalysisProperties properties) {
        return new JavaBackendAnalysisProfile(properties.targetRole());
    }

    @Bean
    ResumeAnalysisProfileDefinition reactFrontendAnalysisProfile() {
        return new ReactFrontendAnalysisProfile();
    }

    @Bean
    ResumeAnalysisProfileRegistry resumeAnalysisProfileRegistry(List<ResumeAnalysisProfileDefinition> definitions) {
        return new ResumeAnalysisProfileRegistry(definitions);
    }

    @Bean ExperienceCalculator experienceCalculator() { return new ExperienceCalculator(); }
    @Bean TechnologyTaxonomy technologyTaxonomy() { return new TechnologyTaxonomy(); }
    @Bean OverallScoreCalculator overallScoreCalculator() { return new OverallScoreCalculator(); }
    @Bean CandidateStrengthCalculator candidateStrengthCalculator() { return new CandidateStrengthCalculator(); }
    @Bean CandidateLevelPolicy candidateLevelPolicy() { return new CandidateLevelPolicy(); }
    @Bean InterviewChancePolicy interviewChancePolicy() { return new InterviewChancePolicy(); }
    @Bean ClaimRiskAnalyzer claimRiskAnalyzer() { return new ClaimRiskAnalyzer(); }
    @Bean GradeFitScorer gradeFitScorer() { return new GradeFitScorer(); }
    @Bean MarketPercentileCalculator marketPercentileCalculator() { return new MarketPercentileCalculator(); }
    @Bean RecommendationRanker recommendationRanker() { return new RecommendationRanker(); }
    @Bean RiskDeduplicator riskDeduplicator() { return new RiskDeduplicator(); }
    @Bean VacancyFitScorer vacancyFitScorer() { return new VacancyFitScorer(); }

    @Bean
    AtsScoreCalculator atsScoreCalculator(AnalysisScoringPolicy policy) {
        return new AtsScoreCalculator(policy);
    }

    @Bean
    SkillNormalizer skillNormalizer(AnalysisTechnologyCatalog catalog) {
        return new SkillNormalizer(catalog);
    }

    @Bean
    EvidenceClassifier evidenceClassifier(SkillNormalizer normalizer) {
        return new EvidenceClassifier(normalizer);
    }

    @Bean
    TechnicalProfileScorer technicalProfileScorer(OverallScoreCalculator calculator) {
        return new TechnicalProfileScorer(calculator);
    }

    @Bean MarketFitScorer marketFitScorer(AnalysisScoringPolicy policy) { return new MarketFitScorer(policy); }
    @Bean SkillGapScorer skillGapScorer(AnalysisScoringPolicy policy) { return new SkillGapScorer(policy); }
    @Bean SkillRoiCalculator skillRoiCalculator(AnalysisScoringPolicy policy) { return new SkillRoiCalculator(policy); }
}
