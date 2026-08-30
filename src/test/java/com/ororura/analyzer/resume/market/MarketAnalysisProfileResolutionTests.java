package com.ororura.analyzer.resume.market;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.ororura.analyzer.resume.ai.ResumeAnalysisProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketAnalysisProfileResolutionTests {

    @Test
    void storePublishesRotatesAndIgnoresSameVersion() {
        InMemoryMarketAnalysisProfileStore store = new InMemoryMarketAnalysisProfileStore();
        MarketAnalysisProfile first = profile("v1", 1);
        MarketAnalysisProfile equivalent = profile("v1", 99);
        MarketAnalysisProfile second = profile("v2", 2);

        store.publish(first);
        assertThat(store.current(ResumeAnalysisProfile.JAVA_BACKEND)).contains(first);
        assertThat(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).isEmpty();

        store.publish(equivalent);
        assertThat(store.current(ResumeAnalysisProfile.JAVA_BACKEND)).contains(first);
        assertThat(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).isEmpty();

        store.publish(second);
        assertThat(store.current(ResumeAnalysisProfile.JAVA_BACKEND)).contains(second);
        assertThat(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).contains(first);
    }

    @Test
    void providerResolvesCurrentThenPreviousThenFallback() {
        MarketAnalysisProfileStore store = mock(MarketAnalysisProfileStore.class);
        MarketAnalysisProfileFallback fallback = mock(MarketAnalysisProfileFallback.class);
        DefaultMarketAnalysisProfileProvider provider = new DefaultMarketAnalysisProfileProvider(store, fallback);
        MarketAnalysisProfile current = profile("current", 3);
        MarketAnalysisProfile previous = profile("previous", 2);
        MarketAnalysisProfile fallbackProfile = profile("fallback", 0);

        when(store.current(ResumeAnalysisProfile.JAVA_BACKEND)).thenReturn(Optional.of(current));
        assertThat(provider.getCurrent(ResumeAnalysisProfile.JAVA_BACKEND)).isSameAs(current);

        when(store.current(ResumeAnalysisProfile.JAVA_BACKEND)).thenReturn(Optional.empty());
        when(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).thenReturn(Optional.of(previous));
        assertThat(provider.getCurrent(ResumeAnalysisProfile.JAVA_BACKEND)).isSameAs(previous);

        when(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).thenReturn(Optional.empty());
        when(fallback.get(ResumeAnalysisProfile.JAVA_BACKEND)).thenReturn(fallbackProfile);
        assertThat(provider.getCurrent(ResumeAnalysisProfile.JAVA_BACKEND)).isSameAs(fallbackProfile);
    }

    @Test
    void providerContinuesAfterStoreFailures() {
        MarketAnalysisProfileStore store = mock(MarketAnalysisProfileStore.class);
        MarketAnalysisProfileFallback fallback = mock(MarketAnalysisProfileFallback.class);
        DefaultMarketAnalysisProfileProvider provider = new DefaultMarketAnalysisProfileProvider(store, fallback);
        MarketAnalysisProfile previous = profile("previous", 2);
        MarketAnalysisProfile fallbackProfile = profile("fallback", 0);

        when(store.current(ResumeAnalysisProfile.JAVA_BACKEND)).thenThrow(new IllegalStateException("unavailable"));
        when(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).thenReturn(Optional.of(previous));
        assertThat(provider.getCurrent(ResumeAnalysisProfile.JAVA_BACKEND)).isSameAs(previous);

        when(store.previous(ResumeAnalysisProfile.JAVA_BACKEND)).thenThrow(new IllegalStateException("unavailable"));
        when(fallback.get(ResumeAnalysisProfile.JAVA_BACKEND)).thenReturn(fallbackProfile);
        assertThat(provider.getCurrent(ResumeAnalysisProfile.JAVA_BACKEND)).isSameAs(fallbackProfile);
    }

    private static MarketAnalysisProfile profile(String version, int sampleSize) {
        return new MarketAnalysisProfile(ResumeAnalysisProfile.JAVA_BACKEND, "Java Backend Developer",
                List.of(MarketAnalysisProfileModelsTests.criterion("java")),
                List.of(MarketAnalysisProfileModelsTests.requirement("java", 0.8)),
                sampleSize, Instant.EPOCH, version);
    }
}
