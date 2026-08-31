package com.ororura.analyzer.resume.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ororura.analyzer.resume.ai.AiProvider;
import com.ororura.analyzer.resume.ai.AiProviderRegistry;
import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.ai.LlmResponseValidator;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfileRegistry;
import com.ororura.analyzer.resume.ai.CriterionAssessment;
import com.ororura.analyzer.resume.ai.profile.JavaBackendAnalysisProfile;
import com.ororura.analyzer.resume.ai.profile.ReactFrontendAnalysisProfile;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import com.ororura.analyzer.resume.api.VacancyAnalysisMode;
import com.ororura.analyzer.resume.api.VacancyAnalysisRequest;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.config.AiProperties;
import com.ororura.analyzer.resume.domain.*;
import com.ororura.analyzer.resume.pdf.PdfFileValidator;
import com.ororura.analyzer.resume.pdf.PdfTextExtractor;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.resume.market.ConfiguredMarketAnalysisProfileFallback;
import com.ororura.analyzer.resume.market.JavaBackendFallbackMarketProfile;
import com.ororura.analyzer.resume.market.ReactFrontendFallbackMarketProfile;
import com.ororura.analyzer.resume.market.ResolvedMarketAnalysisProfile;
import com.ororura.analyzer.resume.market.MarketProfileSource;
import com.ororura.analyzer.resume.ai.DefaultAnalysisTechnologyCatalog;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileFactory;
import com.ororura.analyzer.resume.market.MarketAnalysisProfileProvider;
import com.ororura.analyzer.vacancy.market.VacancyMarketService;
import com.ororura.analyzer.vacancy.selection.SelectionMode;
import com.ororura.analyzer.vacancy.selection.VacancySelection;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeAnalysisServiceTests {

    @Test
    void keepsAllFinalCalculationsOnTheBackendBoundary() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T07:00:00Z"), ZoneOffset.UTC);
        ResumeAnalysisProperties properties = new ResumeAnalysisProperties(
                DataSize.ofMegabytes(10), "Java Backend Developer", "1", "2026-08", 200_000);
        ResumeAnalysisProfileRegistry profiles = profileRegistry(properties);
        var catalog = new DefaultAnalysisTechnologyCatalog();
        var fallback = new ConfiguredMarketAnalysisProfileFallback(profiles, new MarketAnalysisProfileFactory(),
                List.of(new JavaBackendFallbackMarketProfile(catalog), new ReactFrontendFallbackMarketProfile(catalog)), clock);
        MarketAnalysisProfileProvider marketProfiles = profile ->
                new ResolvedMarketAnalysisProfile(fallback.get(profile), MarketProfileSource.FALLBACK);
        var javaProfile = fallback.get(ResumeAnalysisProfile.JAVA_BACKEND);
        var reactProfile = fallback.get(ResumeAnalysisProfile.REACT_FRONTEND);
        PdfFileValidator fileValidator = mock(PdfFileValidator.class);
        PdfTextExtractor extractor = mock(PdfTextExtractor.class);
        VacancyMarketService marketService = mock(VacancyMarketService.class);
        AiProvider polza = provider(AiProviderType.POLZA, "polza-model");
        AiProvider codex = provider(AiProviderType.CODEX_CLI, null);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf".getBytes());
        byte[] bytes = "validated".getBytes();
        String text = "Java Spring Backend PostgreSQL Docker";
        VacancyMarketData market = market();
        VacancyMarketData reactMarket = reactMarket();
        when(fileValidator.validate(file)).thenReturn(bytes);
        when(extractor.extract(bytes)).thenReturn(text);
        when(marketService.load(ResumeAnalysisProfile.JAVA_BACKEND, "Java Backend Developer")).thenReturn(market);
        when(marketService.load(ResumeAnalysisProfile.REACT_FRONTEND, "React Frontend Developer"))
                .thenReturn(reactMarket);
        VacancyAnalysisRequest selectedRequest = new VacancyAnalysisRequest(VacancyAnalysisMode.SELECTED_VACANCIES,
                null, new VacancySelection(SelectionMode.SELECTED, List.of("hh-1", "hh-2"), null, List.of()));
        VacancyAnalysisRequest singleRequest = new VacancyAnalysisRequest(
                VacancyAnalysisMode.SINGLE_VACANCY, "hh-1", null);
        when(marketService.load(ResumeAnalysisProfile.JAVA_BACKEND, selectedRequest, "Java Backend Developer"))
                .thenReturn(market);
        when(marketService.load(ResumeAnalysisProfile.JAVA_BACKEND, singleRequest, "Java Backend Developer"))
                .thenReturn(market);
        when(polza.analyze(javaProfile, text, market)).thenReturn(llm());
        when(codex.analyze(javaProfile, text, market)).thenReturn(llm());
        when(polza.analyze(reactProfile, text, reactMarket)).thenReturn(reactLlm());
        AiProviderRegistry registry = new AiProviderRegistry(List.of(polza, codex),
                new AiProperties(AiProviderType.POLZA));

        TechnologyTaxonomy taxonomy = new TechnologyTaxonomy();
        AtsScoreCalculator ats = new AtsScoreCalculator();
        ResumeAnalysisService service = new ResumeAnalysisService(fileValidator, extractor, marketService, registry,
                new LlmResponseValidator(clock), new ExperienceCalculator(), taxonomy, ats,
                new OverallScoreCalculator(), new CandidateStrengthCalculator(), new CandidateLevelPolicy(),
                new InterviewChancePolicy(), new ResumeAnalysisAssembler(properties), properties, clock,
                marketProfiles);

        ResumeAnalysisResult result = service.analyze(file, AiProviderType.POLZA);
        ResumeAnalysisResult codexResult = service.analyze(file, AiProviderType.CODEX_CLI);
        ResumeAnalysisResult selectedResult = service.analyze(file, AiProviderType.POLZA, selectedRequest);
        ResumeAnalysisResult singleResult = service.analyze(file, AiProviderType.POLZA, singleRequest);
        ResumeAnalysisResult reactResult = service.analyze(file, AiProviderType.POLZA,
                ResumeAnalysisProfile.REACT_FRONTEND, VacancyAnalysisRequest.autoMarket());

        assertThat(result.experience()).isEqualTo(new ResumeAnalysisResult.Experience(16, 1, 4));
        assertThat(result.scores().commercialExperience()).isEqualTo(7);
        assertThat(result.scores().ats()).isEqualTo(51);
        assertThat(result.overallScore()).isEqualTo(66);
        assertThat(result.candidateStrength()).isEqualTo(69);
        assertThat(result.detectedLevel()).isEqualTo(ResumeAnalysisResult.CandidateLevel.JUNIOR_PLUS);
        assertThat(result.hrScreeningChance()).isEqualTo(ResumeAnalysisResult.InterviewChance.MEDIUM);
        assertThat(result.technicalInterviewChance()).isEqualTo(ResumeAnalysisResult.InterviewChance.MEDIUM);
        assertThat(result.metadata().generatedAt()).isEqualTo(Instant.parse("2026-08-28T07:00:00Z"));
        assertThat(result.metadata().provider()).isEqualTo(AiProviderType.POLZA);
        assertThat(result.metadata().analysisProfile()).isEqualTo(ResumeAnalysisProfile.JAVA_BACKEND);
        assertThat(result.metadata().marketProfileSource()).isEqualTo(MarketProfileSource.FALLBACK);
        assertThat(result.metadata().marketProfileVersion())
                .isEqualTo(fallback.get(ResumeAnalysisProfile.JAVA_BACKEND).version())
                .startsWith("sha256:");
        assertThat(codexResult.metadata().provider()).isEqualTo(AiProviderType.CODEX_CLI);
        assertThat(codexResult.metadata().model()).isNull();
        assertThat(codexResult.scores()).isEqualTo(result.scores());
        assertThat(codexResult.experience()).isEqualTo(result.experience());
        assertThat(codexResult.overallScore()).isEqualTo(result.overallScore());
        assertThat(codexResult.detectedLevel()).isEqualTo(result.detectedLevel());
        assertThat(codexResult.hrScreeningChance()).isEqualTo(result.hrScreeningChance());
        assertThat(codexResult.technicalInterviewChance()).isEqualTo(result.technicalInterviewChance());
        assertThat(selectedResult.scores()).isEqualTo(result.scores());
        assertThat(singleResult.scores()).isEqualTo(result.scores());
        assertThat(singleResult.vacancyFit().score()).isBetween(0, 100);
        assertThat(singleResult.vacancyFit().applyRecommendation()).isNotNull();
        assertThat(singleResult.markdownReport()).contains("# Анализ резюме", "## Соответствие вакансии");
        assertThat(result.vacancyFit()).isNull();
        assertThat(result.marketFit().score()).isBetween(0, 100);
        assertThat(result.skillEvidence().skills()).isNotEmpty();
        assertThat(reactResult.targetRole()).isEqualTo("React Frontend Developer");
        assertThat(reactResult.metadata().marketProfileVersion())
                .isEqualTo(fallback.get(ResumeAnalysisProfile.REACT_FRONTEND).version())
                .isNotEqualTo(result.metadata().marketProfileVersion());
        assertThat(reactResult.scores().assessments()).extracting(CriterionAssessment::criterionId)
                .contains("javascript-fallback", "typescript-fallback", "react-fallback")
                .doesNotContain("java-fallback", "spring-backend-fallback");
    }

    @Test
    void rejectsOversizedExtractedTextBeforeProviderSelection() {
        Clock clock = Clock.systemUTC();
        ResumeAnalysisProperties properties = new ResumeAnalysisProperties(
                DataSize.ofMegabytes(10), "Java Backend Developer", "1", "2026-08", 4);
        PdfFileValidator fileValidator = mock(PdfFileValidator.class);
        PdfTextExtractor extractor = mock(PdfTextExtractor.class);
        AiProviderRegistry registry = mock(AiProviderRegistry.class);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf".getBytes());
        byte[] bytes = "validated".getBytes();
        when(fileValidator.validate(file)).thenReturn(bytes);
        when(extractor.extract(bytes)).thenReturn("12345");
        ResumeAnalysisService service = new ResumeAnalysisService(fileValidator, extractor,
                mock(VacancyMarketService.class), registry, mock(LlmResponseValidator.class),
                mock(ExperienceCalculator.class), mock(TechnologyTaxonomy.class), mock(AtsScoreCalculator.class),
                mock(OverallScoreCalculator.class), mock(CandidateStrengthCalculator.class),
                mock(CandidateLevelPolicy.class), mock(InterviewChancePolicy.class),
                mock(ResumeAnalysisAssembler.class), properties, clock, mock(MarketAnalysisProfileProvider.class));

        assertThatThrownBy(() -> service.analyze(file, AiProviderType.CODEX_CLI))
                .isInstanceOf(ResumeAnalysisException.class)
                .extracting(error -> ((ResumeAnalysisException) error).getCode())
                .isEqualTo(ResumeErrorCode.RESUME_TEXT_TOO_LARGE);
        verifyNoInteractions(registry);
    }

    private static AiProvider provider(AiProviderType type, String model) {
        AiProvider provider = mock(AiProvider.class);
        when(provider.type()).thenReturn(type);
        when(provider.isAvailable()).thenReturn(true);
        when(provider.model()).thenReturn(Optional.ofNullable(model));
        return provider;
    }

    private static LlmResumeAnalysisResponse llm() {
        return new LlmResumeAnalysisResponse(
                List.of(assessment("java-fallback", 8), assessment("spring-backend-fallback", 7),
                        assessment("backend-architecture-fallback", 8), assessment("persistence-fallback", 7),
                        assessment("infrastructure-fallback", 6), assessment("testing-fallback", 6)),
                new LlmResumeAnalysisResponse.ExperienceAssessment(score(7), score(8), score(6)),
                new LlmResumeAnalysisResponse.ResumeAssessment(score(7), score(8)),
                new LlmResumeAnalysisResponse.Skills(
                        List.of("Java", "PostgreSQL"), List.of("Docker"), List.of("Kubernetes")),
                List.of(new LlmResumeAnalysisResponse.EmploymentPeriod(
                        "Example", "Java Developer", 2025, 5, null, null, true)),
                List.of("strength"), List.of("weakness"), List.of("issue"), List.of("recommendation"), List.of());
    }

    private static LlmResumeAnalysisResponse reactLlm() {
        return new LlmResumeAnalysisResponse(
                List.of(assessment("javascript-fallback", 7), assessment("typescript-fallback", 7),
                        assessment("react-fallback", 8), assessment("frontend-platform-fallback", 8),
                        assessment("state-management-fallback", 6), assessment("frontend-testing-fallback", 5)),
                new LlmResumeAnalysisResponse.ExperienceAssessment(score(7), score(8), score(6)),
                new LlmResumeAnalysisResponse.ResumeAssessment(score(7), score(8)),
                new LlmResumeAnalysisResponse.Skills(List.of("React", "TypeScript"), List.of(), List.of()),
                List.of(new LlmResumeAnalysisResponse.EmploymentPeriod(
                        "Example", "Frontend Developer", 2025, 5, null, null, true)),
                List.of("strength"), List.of("weakness"), List.of("issue"), List.of("recommendation"), List.of());
    }

    private static LlmResumeAnalysisResponse.SemanticScore score(int value) {
        return new LlmResumeAnalysisResponse.SemanticScore(value, List.of("evidence"));
    }

    private static CriterionAssessment assessment(String criterion, int value) {
        return new CriterionAssessment(criterion, value, List.of("evidence"));
    }

    private static ResumeAnalysisProfileRegistry profileRegistry(ResumeAnalysisProperties properties) {
        return new ResumeAnalysisProfileRegistry(List.of(
                new JavaBackendAnalysisProfile(properties), new ReactFrontendAnalysisProfile()));
    }

    private static VacancyMarketData market() {
        return new VacancyMarketData("test", 10,
                Map.of("Java", 1.0, "Spring Boot", .8, "PostgreSQL", .6,
                        "Backend development", .9, "Docker", .5, "Kubernetes", .2),
                Map.of(), Map.of(), Map.of(), List.of());
    }

    private static VacancyMarketData reactMarket() {
        return new VacancyMarketData("test", 10,
                Map.of("React", 1.0, "TypeScript", .8, "JavaScript", .7),
                Map.of(), Map.of(), Map.of(), List.of());
    }
}
