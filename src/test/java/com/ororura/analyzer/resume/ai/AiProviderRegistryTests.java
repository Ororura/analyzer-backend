package com.ororura.analyzer.resume.ai;

import java.util.List;

import com.ororura.analyzer.resume.config.AiProperties;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderRegistryTests {

    @Test
    void resolvesExplicitAndDefaultProviders() {
        AiProvider polza = provider(AiProviderType.POLZA, true);
        AiProvider codex = provider(AiProviderType.CODEX_CLI, true);
        AiProviderRegistry registry = new AiProviderRegistry(List.of(polza, codex),
                new AiProperties(AiProviderType.POLZA));

        assertThat(registry.get(null)).isSameAs(polza);
        assertThat(registry.get(AiProviderType.POLZA)).isSameAs(polza);
        assertThat(registry.get(AiProviderType.CODEX_CLI)).isSameAs(codex);
    }

    @Test
    void rejectsUnavailableProviderWithoutFallback() {
        AiProvider polza = provider(AiProviderType.POLZA, true);
        AiProvider codex = provider(AiProviderType.CODEX_CLI, false);
        AiProviderRegistry registry = new AiProviderRegistry(List.of(polza, codex),
                new AiProperties(AiProviderType.POLZA));

        assertThatThrownBy(() -> registry.get(AiProviderType.CODEX_CLI))
                .isInstanceOf(ResumeAnalysisException.class)
                .extracting(error -> ((ResumeAnalysisException) error).getCode())
                .isEqualTo(ResumeErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    @Test
    void rejectsDuplicateProviderRegistration() {
        assertThatThrownBy(() -> new AiProviderRegistry(
                List.of(provider(AiProviderType.POLZA, true), provider(AiProviderType.POLZA, true)),
                new AiProperties(AiProviderType.POLZA)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static AiProvider provider(AiProviderType type, boolean available) {
        AiProvider provider = mock(AiProvider.class);
        when(provider.type()).thenReturn(type);
        when(provider.isAvailable()).thenReturn(available);
        return provider;
    }
}
