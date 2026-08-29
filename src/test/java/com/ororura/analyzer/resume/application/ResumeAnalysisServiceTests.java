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
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.config.AiProperties;
import com.ororura.analyzer.resume.domain.*;
import com.ororura.analyzer.resume.pdf.PdfFileValidator;
import com.ororura.analyzer.resume.pdf.PdfTextExtractor;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.vacancy.VacancyMarketService;
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
        PdfFileValidator fileValidator = mock(PdfFileValidator.class);
        PdfTextExtractor extractor = mock(PdfTextExtractor.class);
        VacancyMarketService marketService = mock(VacancyMarketService.class);
        AiProvider polza = provider(AiProviderType.POLZA, "polza-model");
        AiProvider codex = provider(AiProviderType.CODEX_CLI, null);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf".getBytes());
        byte[] bytes = "validated".getBytes();
        String text = "Java Spring Backend PostgreSQL Docker";
        VacancyMarketData market = market();
        when(fileValidator.validate(file)).thenReturn(bytes);
        when(extractor.extract(bytes)).thenReturn(text);
        when(marketService.load("Java Backend Developer")).thenReturn(market);
        when(polza.analyze(text, market)).thenReturn(llm());
        when(codex.analyze(text, market)).thenReturn(llm());
        AiProviderRegistry registry = new AiProviderRegistry(List.of(polza, codex),
                new AiProperties(AiProviderType.POLZA));

        TechnologyTaxonomy taxonomy = new TechnologyTaxonomy();
        AtsScoreCalculator ats = new AtsScoreCalculator(taxonomy);
        ResumeAnalysisService service = new ResumeAnalysisService(fileValidator, extractor, marketService, registry,
                new LlmResponseValidator(clock), new ExperienceCalculator(), taxonomy, ats,
                new OverallScoreCalculator(), new CandidateStrengthCalculator(), new CandidateLevelPolicy(),
                new InterviewChancePolicy(), new ResumeAnalysisAssembler(properties), properties, clock);

        ResumeAnalysisResult result = service.analyze(file, AiProviderType.POLZA);
        ResumeAnalysisResult codexResult = service.analyze(file, AiProviderType.CODEX_CLI);

        assertThat(result.experience()).isEqualTo(new ResumeAnalysisResult.Experience(16, 1, 4));
        assertThat(result.scores().commercialExperience()).isEqualTo(7);
        assertThat(result.scores().ats()).isEqualTo(64);
        assertThat(result.overallScore()).isEqualTo(68);
        assertThat(result.candidateStrength()).isEqualTo(69);
        assertThat(result.detectedLevel()).isEqualTo(ResumeAnalysisResult.CandidateLevel.JUNIOR_PLUS);
        assertThat(result.hrScreeningChance()).isEqualTo(ResumeAnalysisResult.InterviewChance.MEDIUM);
        assertThat(result.technicalInterviewChance()).isEqualTo(ResumeAnalysisResult.InterviewChance.MEDIUM);
        assertThat(result.metadata().generatedAt()).isEqualTo(Instant.parse("2026-08-28T07:00:00Z"));
        assertThat(result.metadata().provider()).isEqualTo(AiProviderType.POLZA);
        assertThat(codexResult.metadata().provider()).isEqualTo(AiProviderType.CODEX_CLI);
        assertThat(codexResult.metadata().model()).isNull();
        assertThat(codexResult.scores()).isEqualTo(result.scores());
        assertThat(codexResult.experience()).isEqualTo(result.experience());
        assertThat(codexResult.overallScore()).isEqualTo(result.overallScore());
        assertThat(codexResult.detectedLevel()).isEqualTo(result.detectedLevel());
        assertThat(codexResult.hrScreeningChance()).isEqualTo(result.hrScreeningChance());
        assertThat(codexResult.technicalInterviewChance()).isEqualTo(result.technicalInterviewChance());
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
                mock(ResumeAnalysisAssembler.class), properties, clock);

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
                new LlmResumeAnalysisResponse.TechnicalAssessment(
                        score(8), score(7), score(8), score(7), score(6), score(6), score(7), score(6)),
                new LlmResumeAnalysisResponse.ExperienceAssessment(score(7), score(8), score(6)),
                new LlmResumeAnalysisResponse.ResumeAssessment(score(7), score(8)),
                new LlmResumeAnalysisResponse.Skills(List.of("Java", "Postgres"), List.of("Docker"), List.of("K8s")),
                List.of(new LlmResumeAnalysisResponse.EmploymentPeriod(
                        "Example", "Java Developer", 2025, 5, null, null, true)),
                List.of("strength"), List.of("weakness"), List.of("issue"), List.of("recommendation"), List.of());
    }

    private static LlmResumeAnalysisResponse.SemanticScore score(int value) {
        return new LlmResumeAnalysisResponse.SemanticScore(value, List.of("evidence"));
    }

    private static VacancyMarketData market() {
        return new VacancyMarketData("test", 10,
                Map.of("Java", 1.0, "Spring Boot", .8, "PostgreSQL", .6,
                        "Backend development", .9, "Docker", .5, "Kubernetes", .2),
                Map.of(), Map.of(), Map.of(), List.of());
    }
}
