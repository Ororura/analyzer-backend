package com.ororura.analyzer.resume.api;

import java.util.List;

import com.ororura.analyzer.resume.application.port.AiProviderType;

public record AiProvidersResponse(AiProviderType defaultProvider, List<Provider> providers) {

    public AiProvidersResponse {
        providers = List.copyOf(providers);
    }

    public record Provider(AiProviderType id, boolean available) {
    }
}
