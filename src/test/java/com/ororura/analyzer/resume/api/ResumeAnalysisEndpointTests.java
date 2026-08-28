package com.ororura.analyzer.resume.api;

import java.time.Instant;
import java.util.List;

import com.ororura.analyzer.resume.application.ResumeAnalysisService;
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
import static org.mockito.Mockito.when;
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
        when(service.analyze(any())).thenReturn(result());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-test".getBytes());

        mockMvc.perform(multipart("/api/resume/analyze").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRole").value("Java Backend Developer"))
                .andExpect(jsonPath("$.detectedLevel").value("junior_plus"))
                .andExpect(jsonPath("$.hrScreeningChance").value("HIGH"))
                .andExpect(jsonPath("$.metadata.generatedAt").value("2026-08-28T07:00:00Z"));
    }

    @Test
    void mapsMissingFileAndProviderFailure() throws Exception {
        mockMvc.perform(multipart("/api/resume/analyze"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE"));

        when(service.analyze(any())).thenThrow(new ResumeAnalysisException(
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
                .andExpect(jsonPath("$.components.schemas.ResumeAnalysisResult").exists());
    }

    private static ResumeAnalysisResult result() {
        return new ResumeAnalysisResult("Java Backend Developer", ResumeAnalysisResult.CandidateLevel.JUNIOR_PLUS,
                new ResumeAnalysisResult.Scores(8, 7, 8, 7, 6, 6, 7, 6, 7, 8, 78, 70),
                76, 77, ResumeAnalysisResult.InterviewChance.HIGH, ResumeAnalysisResult.InterviewChance.HIGH,
                new ResumeAnalysisResult.Experience(16, 1, 4),
                new ResumeAnalysisResult.Skills(List.of("Java"), List.of("Docker"), List.of("Kubernetes")),
                List.of("strength"), List.of("weakness"), List.of("issue"), List.of("recommendation"),
                new ResumeAnalysisResult.Market("test", 20),
                new ResumeAnalysisResult.Metadata("1", "2026-08", Instant.parse("2026-08-28T07:00:00Z"), "test-model"),
                List.of());
    }
}
