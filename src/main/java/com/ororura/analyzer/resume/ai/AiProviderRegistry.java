package com.ororura.analyzer.resume.ai;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.ororura.analyzer.resume.config.AiProperties;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.springframework.stereotype.Component;

@Component
public class AiProviderRegistry {

    private final Map<AiProviderType, AiProvider> providers;
    private final AiProperties properties;

    public AiProviderRegistry(List<AiProvider> providers, AiProperties properties) {
        EnumMap<AiProviderType, AiProvider> registered = new EnumMap<>(AiProviderType.class);
        for (AiProvider provider : providers) {
            if (registered.putIfAbsent(provider.type(), provider) != null) {
                throw new IllegalStateException("Duplicate AI provider registration: " + provider.type());
            }
        }
        this.providers = Collections.unmodifiableMap(registered);
        this.properties = properties;
    }

    public AiProvider get(AiProviderType requested) {
        AiProviderType selected = requested == null ? properties.defaultProvider() : requested;
        AiProvider provider = providers.get(selected);
        if (provider == null || !provider.isAvailable()) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "AI provider is unavailable");
        }
        return provider;
    }

    public AiProviderType defaultProvider() {
        return properties.defaultProvider();
    }

    public boolean isAvailable(AiProviderType type) {
        AiProvider provider = providers.get(type);
        return provider != null && provider.isAvailable();
    }
}
