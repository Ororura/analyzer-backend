package com.ororura.analyzer.vacancy.application.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.vacancy.sync.enabled", havingValue = "true", matchIfMissing = true)
public class VacancySyncScheduler {
    private final VacancySyncService syncService;

    public VacancySyncScheduler(VacancySyncService syncService) { this.syncService = syncService; }

    @Scheduled(fixedDelayString = "${app.vacancy.sync.interval:30m}",
            initialDelayString = "${app.vacancy.sync.initial-delay:10s}")
    public void synchronize() { syncService.synchronize(); }
}
