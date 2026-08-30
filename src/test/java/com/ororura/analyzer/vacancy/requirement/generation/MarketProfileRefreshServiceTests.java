package com.ororura.analyzer.vacancy.requirement.generation;

import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class MarketProfileRefreshServiceTests {
    @Test
    void regeneratesOnceAndReusesSameQuantizedMarketState() {
        MarketAnalysisProfileGenerationService generation = mock(MarketAnalysisProfileGenerationService.class);
        MarketAnalysisProfile profile = mock(MarketAnalysisProfile.class);
        when(profile.profile()).thenReturn(com.ororura.analyzer.resume.ai.ResumeAnalysisProfile.JAVA_BACKEND);
        when(profile.criteria()).thenReturn(MarketCriterionFixture.groups().stream()
                .map(value -> new com.ororura.analyzer.resume.ai.AnalysisCriterion(
                        value.label(), value.label(), value.description(), 1.0 / 6, .5)).toList());
        when(profile.version()).thenReturn("sha256:profile");
        when(generation.generateAndPublish(MarketCriterionFixture.statistics())).thenReturn(profile);
        MarketProfileRefreshService service = new MarketProfileRefreshService(
                new MarketStateFingerprintFactory(), generation);

        assertThat(service.refresh(MarketCriterionFixture.statistics()).status())
                .isEqualTo(MarketProfileRefreshService.Status.REGENERATED);
        assertThat(service.refresh(MarketCriterionFixture.statistics()).status())
                .isEqualTo(MarketProfileRefreshService.Status.REUSED_UNCHANGED);
        verify(generation).generateAndPublish(MarketCriterionFixture.statistics());
        verifyNoMoreInteractions(generation);
    }
}
