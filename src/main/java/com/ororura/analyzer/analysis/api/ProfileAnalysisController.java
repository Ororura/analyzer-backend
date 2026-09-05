package com.ororura.analyzer.analysis.api;

import java.util.UUID;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.ororura.analyzer.analysis.application.*;
import com.ororura.analyzer.analysis.domain.AnalysisRun;
import com.ororura.analyzer.resume.ai.*;
import com.ororura.analyzer.resume.api.VacancyAnalysisRequest;
import com.ororura.analyzer.resume.api.VacancyAnalysisMode;

@RestController
public class ProfileAnalysisController {
    private final ProfileAnalysisService service;
    private final AnalysisRunStore runs;
    public ProfileAnalysisController(ProfileAnalysisService service,AnalysisRunStore runs) { this.service=service;this.runs=runs; }
    @PostMapping(value="/api/resume/analyze",params="profileId",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisRun> analyze(@RequestPart("file") MultipartFile file,@RequestParam UUID profileId,
            @RequestParam(required=false) AiProviderType provider,@RequestParam(required=false) ResumeAnalysisProfile profile,
            @RequestPart(name="analysis",required=false) VacancyAnalysisRequest analysis,HttpSession session) {
        if(profile!=null || analysis!=null && analysis.mode()!=null && analysis.mode()!=VacancyAnalysisMode.AUTO_MARKET)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"profileId supports AUTO_MARKET; do not combine with a legacy profile or selection");
        var run=service.analyze(ProfileSession.owner(session),profileId,file,provider);
        return ResponseEntity.ok().header("X-Analysis-Run-Id",run.id().toString()).body(run);
    }
    @GetMapping("/api/analysis-runs/{id}")
    public AnalysisRun get(@PathVariable UUID id,HttpSession session) { return runs.get(ProfileSession.owner(session),id); }
}
