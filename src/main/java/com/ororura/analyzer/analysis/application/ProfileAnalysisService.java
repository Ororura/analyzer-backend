package com.ororura.analyzer.analysis.application;

import java.security.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.analysis.market.MarketSnapshotBuilder;
import com.ororura.analyzer.resume.application.ResumeAnalysisService;
import com.ororura.analyzer.resume.ai.AiProviderType;
import com.ororura.analyzer.resume.pdf.PdfFileValidator;
import com.ororura.analyzer.resume.error.*;

@Service
public class ProfileAnalysisService {
    private final AnalysisProfileService profiles;
    private final MarketSnapshotBuilder snapshots;
    private final EffectiveAnalysisConfigResolver configs;
    private final AnalysisRunStore runs;
    private final ResumeAnalysisService analyzer;
    private final PdfFileValidator validator;
    private final com.ororura.analyzer.resume.ai.ResumeAnalysisPromptFactory prompts;
    public ProfileAnalysisService(AnalysisProfileService profiles,MarketSnapshotBuilder snapshots,
            EffectiveAnalysisConfigResolver configs,AnalysisRunStore runs,ResumeAnalysisService analyzer,PdfFileValidator validator,com.ororura.analyzer.resume.ai.ResumeAnalysisPromptFactory prompts) {
        this.profiles=profiles;this.snapshots=snapshots;this.configs=configs;this.runs=runs;this.analyzer=analyzer;this.validator=validator;this.prompts=prompts;
    }
    public AnalysisRun analyze(UUID owner,UUID profileId,MultipartFile file,AiProviderType provider) {
        var profile=profiles.get(owner,profileId);
        byte[] bytes=validator.validate(file);
        UUID id=runs.start(owner,profile,hash(bytes));
        try {
            var snapshot=snapshots.build(profile.segment());
            var config=configs.resolve(profile,snapshot,provider);
            runs.pin(owner,id,snapshot,config);
            var result=analyzer.analyze(file,config,(text,llm) -> runs.evidence(owner,id,text,llm,prompts.create(config,text)));
            return runs.complete(owner,id,result);
        } catch(RuntimeException e) {
            String code=e instanceof ResumeAnalysisException ex ? ex.getCode().name() : "ANALYSIS_FAILED";
            runs.fail(owner,id,code);
            throw new ProfileAnalysisFailure(id,e);
        }
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static class ProfileAnalysisFailure extends RuntimeException {
        private final UUID runId;
        public ProfileAnalysisFailure(UUID runId,RuntimeException cause) { super("Profile analysis failed",cause);this.runId=runId; }
        public UUID runId() { return runId; }
    }
}
