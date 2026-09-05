package com.ororura.analyzer.analysis.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;

@Entity @Table(name="analysis_runs")
public class AnalysisRunEntity {
    @Id private UUID id;
    @Version private Long version;
    @Column(nullable=false,updatable=false) private UUID ownerId;
    @Column(nullable=false,updatable=false) private UUID profileId;
    @Column(nullable=false,updatable=false) private long profileVersion;
    @Column(nullable=false,updatable=false,length=64) private String inputSha256;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private AnalysisRun.Status status;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private CandidateGrade targetGrade;
    @Enumerated(EnumType.STRING) @Column(length=32) private CandidateGrade detectedGrade;
    @Column(nullable=false,updatable=false) private Instant createdAt;
    private Instant completedAt;
    @Column(length=64) private String failureCode;
    private UUID marketSnapshotId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,updatable=false,columnDefinition="jsonb") private UserAnalysisProfile profileSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private EffectiveAnalysisConfig effectiveConfig;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private ResumeAnalysisResult result;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private LlmResumeAnalysisResponse llmResponse;
    @Column(columnDefinition="text") private String extractedText;
    @Column(columnDefinition="text") private String systemInstruction;
    @Column(columnDefinition="text") private String promptInput;
    @Column(columnDefinition="text") private String responseSchema;
    protected AnalysisRunEntity() {}
    public AnalysisRunEntity(UUID owner,UserAnalysisProfile profile,String inputSha256,Instant now) {
        id=UUID.randomUUID();ownerId=owner;profileId=profile.id();profileVersion=profile.version();
        this.inputSha256=inputSha256;createdAt=now;profileSnapshot=profile;targetGrade=profile.targetGrade();status=AnalysisRun.Status.PREPARING;
    }
    public UUID id() { return id; }
    public void pin(EffectiveAnalysisConfig config) {
        if(status!=AnalysisRun.Status.PREPARING || effectiveConfig!=null || !profileSnapshot.equals(config.profile()))
            throw new IllegalStateException("Run config is already pinned or profile differs");
        effectiveConfig=config;marketSnapshotId=config.marketSnapshotId();status=AnalysisRun.Status.RUNNING;
    }
    public void evidence(String text,LlmResumeAnalysisResponse response,com.ororura.analyzer.resume.ai.ResumeAnalysisPrompt prompt) {
        if(status!=AnalysisRun.Status.RUNNING || llmResponse!=null) throw new IllegalStateException("Invalid evidence transition");
        extractedText=text;llmResponse=response;systemInstruction=prompt.systemInstruction();
        promptInput=prompt.input().toString();responseSchema=prompt.schema().toString();
    }
    public void complete(ResumeAnalysisResult value,Instant now) {
        if(status!=AnalysisRun.Status.RUNNING) throw new IllegalStateException("Run is not running");
        result=value;detectedGrade=value.detectedGrade();status=AnalysisRun.Status.COMPLETED;completedAt=now;
    }
    public void fail(String code,Instant now) {
        if(status==AnalysisRun.Status.COMPLETED || status==AnalysisRun.Status.FAILED) return;
        status=AnalysisRun.Status.FAILED;failureCode=code;completedAt=now;
    }
    public AnalysisRun domain() { return new AnalysisRun(id,profileId,profileVersion,status,createdAt,completedAt,
            failureCode,targetGrade,detectedGrade,effectiveConfig,result); }
}
