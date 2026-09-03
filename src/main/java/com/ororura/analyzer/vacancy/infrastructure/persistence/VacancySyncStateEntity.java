package com.ororura.analyzer.vacancy.infrastructure.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vacancy_sync_state")
public class VacancySyncStateEntity {
    @Id
    @Column(length = 64)
    private String source;
    @Column(name = "last_started_at")
    private OffsetDateTime lastStartedAt;
    @Column(name = "last_successful_at")
    private OffsetDateTime lastSuccessfulAt;
    @Column(name = "last_finished_at")
    private OffsetDateTime lastFinishedAt;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "market_version", nullable = false)
    private long marketVersion;

    protected VacancySyncStateEntity() {
    }

    public VacancySyncStateEntity(String source) {
        this.source = source;
        this.status = "NEVER_RUN";
    }

    public void started(OffsetDateTime now) {
        lastStartedAt = now;
        status = "RUNNING";
        lastError = null;
    }

    public void finished(OffsetDateTime now, boolean successful, String error) {
        lastFinishedAt = now;
        if (successful) lastSuccessfulAt = now;
        status = successful ? "SUCCESS" : "FAILED";
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
    }

    public void incrementMarketVersion() { marketVersion++; }
    public long marketVersion() { return marketVersion; }
}
