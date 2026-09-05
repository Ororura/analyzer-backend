package com.ororura.analyzer.analysis.application;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.ororura.analyzer.analysis.domain.*;
import com.ororura.analyzer.analysis.persistence.*;
import com.ororura.analyzer.resume.ai.LlmResumeAnalysisResponse;
import com.ororura.analyzer.resume.api.ResumeAnalysisResult;

/** Short transactions only; no PDF processing or network calls while holding a transaction. */
@Service @Transactional
public class AnalysisRunStore {
    private final AnalysisRunRepository runs;
    private final MarketSnapshotRepository snapshots;
    private final Clock clock;
    public AnalysisRunStore(AnalysisRunRepository runs,MarketSnapshotRepository snapshots,Clock clock) {
        this.runs=runs;this.snapshots=snapshots;this.clock=clock;
    }
    public UUID start(UUID owner,UserAnalysisProfile profile,String sha256) {
        return runs.saveAndFlush(new AnalysisRunEntity(owner,profile,sha256,clock.instant())).id();
    }
    public void pin(UUID owner,UUID runId,MarketSnapshot snapshot,EffectiveAnalysisConfig config) {
        if(!snapshot.id().equals(config.marketSnapshotId())) throw new IllegalArgumentException("Snapshot mismatch");
        snapshots.save(new MarketSnapshotEntity(snapshot));find(owner,runId).pin(config);runs.flush();
    }
    public void evidence(UUID owner,UUID runId,String text,LlmResumeAnalysisResponse response,
            com.ororura.analyzer.resume.ai.ResumeAnalysisPrompt prompt) { find(owner,runId).evidence(text,response,prompt); }
    public AnalysisRun complete(UUID owner,UUID id,ResumeAnalysisResult result) {
        var run=find(owner,id);run.complete(result,clock.instant());runs.flush();return run.domain();
    }
    public void fail(UUID owner,UUID id,String code) { find(owner,id).fail(code,clock.instant()); }
    @Transactional(readOnly=true)
    public AnalysisRun get(UUID owner,UUID id) { return find(owner,id).domain(); }
    private AnalysisRunEntity find(UUID owner,UUID id) {
        return runs.findByIdAndOwnerId(id,owner).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Run not found"));
    }
}
