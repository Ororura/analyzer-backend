package com.ororura.analyzer.resume.api;

import java.io.IOException;

import com.ororura.analyzer.api.ApiErrorResponse;
import com.ororura.analyzer.resume.application.ResumeAnalysisService;
import com.ororura.analyzer.resume.application.port.AiProviderType;
import com.ororura.analyzer.resume.application.port.ResumeDocument;
import com.ororura.analyzer.analysis.profile.ResumeAnalysisProfile;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.market.application.VacancyMarketRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resume")
public class ResumeAnalysisController {

    private final ResumeAnalysisService service;
    private final ResumeAnalysisApiMapper mapper;

    public ResumeAnalysisController(ResumeAnalysisService service, ResumeAnalysisApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(summary = "Analyze a PDF resume against ATS criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resume analyzed"),
            @ApiResponse(responseCode = "400", description = "Missing or empty file",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "PDF or extracted resume text is too large",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "Unsupported file type",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "PDF cannot be parsed or has no text",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "AI returned an invalid response",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "AI provider unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "504", description = "AI provider timeout",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumeAnalysisResult analyze(
            @RequestPart("file") @Schema(type = "string", format = "binary") MultipartFile file,
            @RequestParam(name = "provider", required = false) AiProviderType provider,
            @RequestParam(name = "profile", required = false) ResumeAnalysisProfile profile,
            @RequestPart(name = "analysis", required = false) VacancyAnalysisRequest analysis) {
        return mapper.toResponse(service.analyze(toDocument(file), provider, profile, toMarketRequest(analysis)));
    }

    private static ResumeDocument toDocument(MultipartFile file) {
        if (file == null) {
            return null;
        }
        try {
            return new ResumeDocument(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getBytes());
        } catch (IOException exception) {
            throw new ResumeAnalysisException(ResumeErrorCode.INVALID_FILE, "Unable to read the uploaded file", exception);
        }
    }

    private static VacancyMarketRequest toMarketRequest(VacancyAnalysisRequest request) {
        if (request == null || request.mode() == null) {
            return VacancyMarketRequest.autoMarket();
        }
        return new VacancyMarketRequest(VacancyMarketRequest.Mode.valueOf(request.mode().name()),
                request.vacancyId(), request.selection());
    }
}
