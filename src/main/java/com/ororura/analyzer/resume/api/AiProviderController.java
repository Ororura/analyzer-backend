package com.ororura.analyzer.resume.api;

import java.util.Arrays;

import com.ororura.analyzer.resume.ai.AiProviderRegistry;
import com.ororura.analyzer.resume.ai.AiProviderType;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/providers")
public class AiProviderController {

    private final AiProviderRegistry registry;

    public AiProviderController(AiProviderRegistry registry) {
        this.registry = registry;
    }

    @Operation(summary = "List configured AI providers and their availability")
    @GetMapping
    public AiProvidersResponse providers() {
        return new AiProvidersResponse(registry.defaultProvider(), Arrays.stream(AiProviderType.values())
                .map(type -> new AiProvidersResponse.Provider(type, registry.isAvailable(type)))
                .toList());
    }
}
