package com.ororura.analyzer.resume.api;

import java.time.Instant;
import java.util.List;

import com.ororura.analyzer.resume.application.ResumeAnalysisService;
import com.ororura.analyzer.resume.ai.CriterionAssessment;
import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeAnalysisEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeAnalysisService service;

    @Test
    void exposesSuccessfulMultipartAnalysis() throws Exception {
        when(service.analyze(any(), nullable(AiProviderType.class), nullable(ResumeAnalysisProfile.class),
                any(VacancyAnalysisRequest.class))).thenReturn(result());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-test".getBytes());

        mockMvc.perform(multipart("/api/resume/analyze").file(file).param("provider", "CODEX_CLI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRole").value("Java Backend Developer"))
                .andExpect(jsonPath("$.detectedLevel").value("junior_plus"))
                .andExpect(jsonPath("$.hrScreeningChance").value("HIGH"))
                .andExpect(jsonPath("$.metadata.generatedAt").value("2026-08-28T07:00:00Z"))
                .andExpect(jsonPath("$.metadata.provider").value("POLZA"));
        verify(service).analyze(any(), eq(AiProviderType.CODEX_CLI), eq(null),
                org.mockito.ArgumentMatchers.argThat(request -> request.mode() == VacancyAnalysisMode.AUTO_MARKET));
    }

    @Test
    void rejectsUnknownProviderEnum() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-test".getBytes());
        mockMvc.perform(multipart("/api/resume/analyze").file(file).param("provider", "user-command"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_QUERY"));
    }

    @Test
    void acceptsExplicitSelectedVacanciesAnalysis() throws Exception {
        when(service.analyze(any(), nullable(AiProviderType.class), nullable(ResumeAnalysisProfile.class),
                any(VacancyAnalysisRequest.class)))
                .thenReturn(result());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-test".getBytes());
        MockMultipartFile analysis = new MockMultipartFile("analysis", "", "application/json", """
                {"mode":"SELECTED_VACANCIES","selection":{"mode":"SELECTED","vacancyIds":["hh-1","hh-2"]}}
                """.getBytes());

        mockMvc.perform(multipart("/api/resume/analyze").file(file).file(analysis))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(76));

        verify(service).analyze(any(), eq(null), eq(null), org.mockito.ArgumentMatchers.argThat(request ->
                request.mode() == VacancyAnalysisMode.SELECTED_VACANCIES
                        && request.selection().vacancyIds().equals(List.of("hh-1", "hh-2"))));
    }

    @Test
    void passesExplicitAnalysisProfileToService() throws Exception {
        when(service.analyze(any(), nullable(AiProviderType.class), eq(ResumeAnalysisProfile.REACT_FRONTEND),
                any(VacancyAnalysisRequest.class))).thenReturn(result());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-test".getBytes());

        mockMvc.perform(multipart("/api/resume/analyze").file(file).param("profile", "REACT_FRONTEND"))
                .andExpect(status().isOk());

        verify(service).analyze(any(), eq(null), eq(ResumeAnalysisProfile.REACT_FRONTEND),
                org.mockito.ArgumentMatchers.argThat(request -> request.mode() == VacancyAnalysisMode.AUTO_MARKET));
    }

    @Test
    void exposesProviderAvailabilityWithoutSensitiveReasons() throws Exception {
        mockMvc.perform(get("/api/ai/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultProvider").value("POLZA"))
                .andExpect(jsonPath("$.providers[0].id").value("POLZA"))
                .andExpect(jsonPath("$.providers[1].id").value("CODEX_CLI"))
                .andExpect(jsonPath("$.providers[0].reason").doesNotExist());
    }

    @Test
    void mapsMissingFileAndProviderFailure() throws Exception {
        mockMvc.perform(multipart("/api/resume/analyze"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE"));

        when(service.analyze(any(), nullable(AiProviderType.class), nullable(ResumeAnalysisProfile.class),
                any(VacancyAnalysisRequest.class))).thenThrow(new ResumeAnalysisException(
                ResumeErrorCode.AI_PROVIDER_UNAVAILABLE, "AI provider is unavailable"));
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-test".getBytes());
        mockMvc.perform(multipart("/api/resume/analyze").file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("AI_PROVIDER_UNAVAILABLE"));
    }

    @Test
    void publishesEndpointAndSchemasInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/resume/analyze'].post").exists())
                .andExpect(jsonPath("$.paths['/api/ai/providers'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ResumeAnalysisResult").exists());
    }

    private static ResumeAnalysisResult result() {
        return new ResumeAnalysisResult("Java Backend Developer", ResumeAnalysisResult.CandidateLevel.JUNIOR_PLUS,
                new ResumeAnalysisResult.Scores(
                        List.of(new CriterionAssessment("criterion-fixture", 8, List.of("evidence"))), 7, 8, 78, 70),
                76, 77, ResumeAnalysisResult.InterviewChance.HIGH, ResumeAnalysisResult.InterviewChance.HIGH,
                new ResumeAnalysisResult.Experience(16, 1, 4),
                new ResumeAnalysisResult.Skills(List.of("Java"), List.of("Docker"), List.of("Kubernetes")),
                List.of("strength"), List.of("weakness"), List.of("issue"), List.of("recommendation"),
                new ResumeAnalysisResult.Market("test", 20),
                new ResumeAnalysisResult.Metadata("1", "2026-08", Instant.parse("2026-08-28T07:00:00Z"),
                        AiProviderType.POLZA, "test-model"),
                List.of());
    }
}
