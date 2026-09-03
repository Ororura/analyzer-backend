package com.ororura.analyzer.vacancy.application.sync;

import java.time.Duration;

import com.ororura.analyzer.vacancy.application.port.VacancyProvider;
import com.ororura.analyzer.vacancy.infrastructure.cache.VacancyCacheFacade;
import com.ororura.analyzer.vacancy.infrastructure.cache.VacancyMarketVersionService;
import com.ororura.analyzer.vacancy.application.port.VacancySourceException;
import com.ororura.analyzer.vacancy.infrastructure.persistence.VacancySyncPersistenceService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VacancySyncServiceTests {
    @Test
    void recordsProviderRateLimitWithoutOpeningPersistenceBatch() {
        VacancyProvider provider = mock(VacancyProvider.class);
        VacancySyncPersistenceService persistence = mock(VacancySyncPersistenceService.class);
        when(provider.search(any())).thenThrow(new VacancySourceException(
                "RATE_LIMITED", "rate limited", 429, "30", null));
        VacancySyncService service = new VacancySyncService(provider, persistence,
                new VacancySyncProperties(true, Duration.ofMinutes(30), Duration.ZERO,
                        "Java", null, 2, 50, 50), mock(VacancyCacheFacade.class),
                mock(VacancyMarketVersionService.class),
                mock(com.ororura.analyzer.market.profile.MarketProfileRefreshCoordinator.class),
                new com.ororura.analyzer.analysis.profile.ResumeAnalysisProfileRegistry(java.util.List.of(
                        new com.ororura.analyzer.analysis.profile.definition.JavaBackendAnalysisProfile(),
                        new com.ororura.analyzer.analysis.profile.definition.ReactFrontendAnalysisProfile())));

        VacancySyncStats stats = service.synchronize();

        assertThat(stats.failed()).isOne();
        assertThat(stats.fetched()).isZero();
        verify(persistence).markStarted();
        verify(persistence).markFinished(false, "rate limited");
    }
}
