package com.ororura.analyzer.resume.domain;

import com.ororura.analyzer.analysis.scoring.AnalysisScoringPolicy;
import com.ororura.analyzer.analysis.profile.SkillNormalizer;

import com.ororura.analyzer.analysis.semantic.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.domain.analysis.*;
import com.ororura.analyzer.analysis.profile.DefaultAnalysisTechnologyCatalog;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.analysis.profile.AnalysisCriterion;
import com.ororura.analyzer.analysis.semantic.CriterionAssessment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.resume.ai.*;
import com.ororura.analyzer.resume.api.*;
import com.ororura.analyzer.resume.config.ResumeScoringProperties;
import com.ororura.analyzer.market.domain.*;
import com.ororura.analyzer.market.domain.VacancyMarketData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicMarketAnalysisTests {
    private final DefaultAnalysisTechnologyCatalog catalog = new DefaultAnalysisTechnologyCatalog();

    @Test
    void normalizesAliasesWithoutMergingSpringFamily() {
        SkillNormalizer normalizer = new SkillNormalizer(catalog);
        assertThat(normalizer.normalizeId(ResumeAnalysisProfile.JAVA_BACKEND, "SpringBoot")).isEqualTo("spring_boot");
        assertThat(normalizer.normalizeId(ResumeAnalysisProfile.JAVA_BACKEND, "spring-boot")).isEqualTo("spring_boot");
        assertThat(normalizer.normalizeId(ResumeAnalysisProfile.JAVA_BACKEND, "Spring")).isEqualTo("spring");
        assertThat(normalizer.normalizeId(ResumeAnalysisProfile.JAVA_BACKEND, "Spring Security"))
                .isEqualTo("spring_security");
        assertThat(normalizer.normalizeId(ResumeAnalysisProfile.JAVA_BACKEND, "GraphQL Federation"))
                .isEqualTo("graphql_federation");
    }

    @Test
    void classifiesEvidenceConservatively() {
        EvidenceClassifier classifier = new EvidenceClassifier(new SkillNormalizer(catalog));
        var result = classifier.classify(profile(), llm(List.of(
                fact("Java", evidence("Implemented API", LlmResumeAnalysisResponse.EvidenceContext.COMMERCIAL_TASK, true, true)),
                fact("Spring Boot", evidence("Skills: Spring Boot", LlmResumeAnalysisResponse.EvidenceContext.SKILLS_SECTION, false, false)))));
        assertThat(result.skills()).filteredOn(value -> value.skill().equals("Java")).singleElement()
                .extracting(SkillEvidenceAnalysis.SkillEvidence::status).isEqualTo(SkillEvidenceAnalysis.EvidenceStatus.STRONG);
        assertThat(result.skills()).filteredOn(value -> value.skill().equals("Spring Boot")).singleElement()
                .extracting(SkillEvidenceAnalysis.SkillEvidence::status).isEqualTo(SkillEvidenceAnalysis.EvidenceStatus.MENTION_ONLY);
        assertThat(result.skills()).filteredOn(value -> value.skill().equals("GraphQL")).singleElement()
                .extracting(SkillEvidenceAnalysis.SkillEvidence::status).isEqualTo(SkillEvidenceAnalysis.EvidenceStatus.NOT_FOUND);
    }

    @Test
    void calculatesMarketFitGapAndRoiWithoutPromotingRareSkill() {
        var evidence = new SkillEvidenceAnalysis(List.of(
                skill("java", "Java", SkillEvidenceAnalysis.EvidenceStatus.STRONG),
                skill("spring_boot", "Spring Boot", SkillEvidenceAnalysis.EvidenceStatus.MEDIUM),
                skill("graphql", "GraphQL", SkillEvidenceAnalysis.EvidenceStatus.NOT_FOUND)));
        ResumeScoringProperties properties = new ResumeScoringProperties();
        var grade = new GradeFitAnalysis(CandidateLevel.JUNIOR_PLUS,
                CandidateLevel.JUNIOR_PLUS, GradeFitAnalysis.GradeFit.MATCH,
                GradeFitAnalysis.Severity.NONE, 100, new ScoreBreakdown(100, List.of()));
        var fit = new MarketFitScorer(properties.toPolicy()).score(profile(), evidence, 7, 80, grade);
        assertThat(fit.score()).isBetween(70, 100);
        assertThat(fit.breakdown().components()).isNotEmpty();
        var gaps = new SkillGapScorer(properties.toPolicy()).score(profile(), evidence);
        assertThat(gaps.gaps()).filteredOn(value -> value.skill().equals("GraphQL")).singleElement()
                .extracting(SkillGapAnalysis.SkillGap::priority).isEqualTo(SkillGapAnalysis.GapPriority.LOW);
        assertThat(new SkillRoiCalculator(AnalysisScoringPolicy.defaults()).calculate(gaps).skills()).allSatisfy(value ->
                assertThat(value.roiScore()).isBetween(0.0, 10.0));
    }

    @Test
    void gradeAndVacancyPoliciesUseBlockersSeparatelyFromOverallScore() {
        VacancyMarketData market = new VacancyMarketData("single_vacancy", 1, Map.of(),
                Map.of("3–6 лет", 1), Map.of(), Map.of(), List.of());
        GradeFitAnalysis grade = new GradeFitScorer().score(CandidateLevel.JUNIOR_PLUS, market);
        assertThat(grade.fit()).isEqualTo(GradeFitAnalysis.GradeFit.SLIGHTLY_UNDERQUALIFIED);
        var fit = new VacancyFitScorer().score(market, new SkillEvidenceAnalysis(List.of()), 12, grade, 90);
        assertThat(fit.blockers()).singleElement().extracting(VacancyFitAnalysis.Blocker::type)
                .isEqualTo(VacancyFitAnalysis.BlockerType.EXPERIENCE);
        assertThat(fit.applyRecommendation()).isEqualTo(VacancyFitAnalysis.ApplyRecommendation.SKIP);
    }

    @Test
    void deduplicatesRisksAndOrdersRecommendationsByPriorityImpactEffort() {
        RiskDeduplicator deduplicator = new RiskDeduplicator();
        var risks = deduplicator.deduplicate(List.of(
                risk(ResumeRisk.RiskSeverity.MEDIUM, "Kafka"), risk(ResumeRisk.RiskSeverity.HIGH, " kafka ")));
        assertThat(risks).singleElement().extracting(ResumeRisk::severity).isEqualTo(ResumeRisk.RiskSeverity.HIGH);

        AtsAnalysis ats = new AtsAnalysis(70, 70, 70, 70, 70, 70, 70, 70,
                List.of(new AtsAnalysis.FieldDiagnostic("education", AtsAnalysis.FieldStatus.FAILED, List.of("missing"))),
                new ScoreBreakdown(70, List.of()));
        var recommendations = new RecommendationRanker().build(ats,
                new SkillGapAnalysis(List.of(new SkillGapAnalysis.SkillGap("graphql", "GraphQL", .06,
                        SkillEvidenceAnalysis.EvidenceStatus.NOT_FOUND, SkillGapAnalysis.GapPriority.LOW, .01))),
                new ResumeClaimRiskAnalysis(List.of()), List.of());
        assertThat(recommendations.items().getFirst().priority())
                .isEqualTo(ResumeRecommendationAnalysis.Priority.HIGH);
    }

    @Test
    void percentileRemainsUnknownWithoutCandidateDistribution() {
        assertThat(new MarketPercentileCalculator().calculate())
                .isEqualTo(new MarketPosition(null, null, null));
    }

    private static MarketAnalysisProfile profile() {
        List<MarketRequirement> requirements = List.of(
                requirement("java", "Java", .9), requirement("spring-boot", "Spring Boot", .7),
                requirement("graphql", "GraphQL", .06));
        List<MarketSkillStatistics> statistics = List.of(
                stat("java", "Java", .9, .8), stat("spring_boot", "Spring Boot", .7, .5),
                stat("graphql", "GraphQL", .06, 0));
        return new MarketAnalysisProfile(ResumeAnalysisProfile.JAVA_BACKEND, "Java Backend Developer",
                List.of(new AnalysisCriterion("technical", "Technical", "Technical evidence", 1, .8)),
                requirements, statistics, List.of(), true, 100, Instant.EPOCH, "v1");
    }

    private static MarketRequirement requirement(String id, String label, double frequency) {
        return new MarketRequirement("technology:" + id, label, RequirementType.TECHNOLOGY, frequency);
    }
    private static MarketSkillStatistics stat(String id, String label, double frequency, double required) {
        return new MarketSkillStatistics(id, label, Math.round(frequency * 100), 100L, frequency,
                required, Math.max(0, frequency - required), 0,
                frequency >= .7 ? SkillImportance.CORE : frequency >= .4 ? SkillImportance.HIGH
                        : frequency >= .2 ? SkillImportance.MEDIUM : SkillImportance.LOW);
    }
    private static SkillEvidenceAnalysis.SkillEvidence skill(String id, String name,
            SkillEvidenceAnalysis.EvidenceStatus status) {
        return new SkillEvidenceAnalysis.SkillEvidence(id, name, status, .8, List.of());
    }
    private static LlmResumeAnalysisResponse.SkillEvidenceFact fact(String skill,
            LlmResumeAnalysisResponse.EvidenceFact... evidence) {
        return new LlmResumeAnalysisResponse.SkillEvidenceFact(skill, List.of(evidence));
    }
    private static LlmResumeAnalysisResponse.EvidenceFact evidence(String text,
            LlmResumeAnalysisResponse.EvidenceContext context, boolean concrete, boolean outcome) {
        return new LlmResumeAnalysisResponse.EvidenceFact(text, context, concrete, outcome);
    }
    private static LlmResumeAnalysisResponse llm(List<LlmResumeAnalysisResponse.SkillEvidenceFact> facts) {
        var score = new LlmResumeAnalysisResponse.SemanticScore(7, List.of("evidence"));
        return new LlmResumeAnalysisResponse(List.of(new CriterionAssessment("technical", 7, List.of("evidence"))),
                new LlmResumeAnalysisResponse.ExperienceAssessment(score, score, score),
                new LlmResumeAnalysisResponse.ResumeAssessment(score, score),
                new LlmResumeAnalysisResponse.Skills(List.of(), List.of(), List.of()), facts, List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
    private static ResumeRisk risk(ResumeRisk.RiskSeverity severity, String key) {
        return new ResumeRisk(ResumeRisk.RiskType.SKILL_GAP, severity, key, key, key, key, key);
    }
}
