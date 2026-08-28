package com.ororura.analyzer.resume.application;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.polza.PolzaProperties;
import com.ororura.analyzer.resume.ai.AiProvider;
import com.ororura.analyzer.resume.ai.LlmResponseValidator;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import com.ororura.analyzer.resume.config.ResumeAnalysisProperties;
import com.ororura.analyzer.resume.domain.*;
import com.ororura.analyzer.resume.pdf.PdfFileValidator;
import com.ororura.analyzer.resume.pdf.PdfTextExtractor;
import com.ororura.analyzer.vacancy.VacancyMarketService;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumeAnalysisServiceTests {

    @Test
    void keepsAllFinalCalculationsOnTheBackendBoundary() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T07:00:00Z"), ZoneOffset.UTC);
        ResumeAnalysisProperties properties = new ResumeAnalysisProperties(
                DataSize.ofMegabytes(10), "Java Backend Developer", "1", "2026-08");
        PolzaProperties polza = new PolzaProperties(URI.create("https://polza.ai/api/v1"),
                "test-model", "secret", Duration.ofSeconds(1));
        PdfFileValidator fileValidator = mock(PdfFileValidator.class);
        PdfTextExtractor extractor = mock(PdfTextExtractor.class);
        VacancyMarketService marketService = mock(VacancyMarketService.class);
        AiProvider provider = mock(AiProvider.class);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "pdf".getBytes());
        byte[] bytes = "validated".getBytes();
        String text = "Java Spring Backend PostgreSQL Docker";
        VacancyMarketData market = market();
        LlmResumeAnalysisResponse llm = llm();
        when(fileValidator.validate(file)).thenReturn(bytes);
        when(extractor.extract(bytes)).thenReturn(text);
        when(marketService.load("Java Backend Developer")).thenReturn(market);
        when(provider.analyze(text, market)).thenReturn(llm);

        TechnologyTaxonomy taxonomy = new TechnologyTaxonomy();
        AtsScoreCalculator ats = new AtsScoreCalculator(taxonomy);
        ResumeAnalysisService service = new ResumeAnalysisService(fileValidator, extractor, marketService, provider,
                new LlmResponseValidator(clock), new ExperienceCalculator(), taxonomy, ats,
                new OverallScoreCalculator(), new CandidateStrengthCalculator(), new CandidateLevelPolicy(),
                new InterviewChancePolicy(), new ResumeAnalysisAssembler(properties, polza), properties, clock);

        ResumeAnalysisResult result = service.analyze(file);

        assertThat(result.experience()).isEqualTo(new ResumeAnalysisResult.Experience(16, 1, 4));
        assertThat(result.scores().commercialExperience()).isEqualTo(7);
        assertThat(result.scores().ats()).isEqualTo(64);
        assertThat(result.overallScore()).isEqualTo(68);
        assertThat(result.candidateStrength()).isEqualTo(69);
        assertThat(result.detectedLevel()).isEqualTo(ResumeAnalysisResult.CandidateLevel.JUNIOR_PLUS);
        assertThat(result.hrScreeningChance()).isEqualTo(ResumeAnalysisResult.InterviewChance.MEDIUM);
        assertThat(result.technicalInterviewChance()).isEqualTo(ResumeAnalysisResult.InterviewChance.MEDIUM);
        assertThat(result.metadata().generatedAt()).isEqualTo(Instant.parse("2026-08-28T07:00:00Z"));
    }

    private static LlmResumeAnalysisResponse llm() {
        return new LlmResumeAnalysisResponse(
                new LlmResumeAnalysisResponse.TechnicalAssessment(8, 7, 8, 7, 6, 6, 7, 6),
                new LlmResumeAnalysisResponse.ExperienceAssessment(7, 8, 6),
                new LlmResumeAnalysisResponse.ResumeAssessment(7, 8),
                new LlmResumeAnalysisResponse.Skills(List.of("Java", "Postgres"), List.of("Docker"), List.of("K8s")),
                List.of(new LlmResumeAnalysisResponse.EmploymentPeriod(
                        "Example", "Java Developer", 2025, 5, null, null, true)),
                List.of("strength"), List.of("weakness"), List.of("issue"), List.of("recommendation"), List.of());
    }

    private static VacancyMarketData market() {
        return new VacancyMarketData("test", 10,
                Map.of("Java", 1.0, "Spring Boot", .8, "PostgreSQL", .6,
                        "Backend development", .9, "Docker", .5, "Kubernetes", .2),
                Map.of(), Map.of(), Map.of(), List.of());
    }
}
