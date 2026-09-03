package com.ororura.analyzer.vacancy.infrastructure.hh;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HhLocationResolverTests {

    @Test
    void resolvesKnownLocationToArea() {
        HhLocationResolver.ResolvedQuery resolved = HhLocationResolver.resolve("Java", "Москва");

        assertThat(resolved.text()).isEqualTo("Java");
        assertThat(resolved.area()).isEqualTo("1");
        assertThat(resolved.fallbackLocation()).isNull();
    }

    @Test
    void preservesNumericArea() {
        assertThat(HhLocationResolver.resolve("Java", "88").area()).isEqualTo("88");
    }

    @Test
    void appendsUnknownLocationAndEnablesLocalFallback() {
        HhLocationResolver.ResolvedQuery resolved = HhLocationResolver.resolve("Java", "Тверь");

        assertThat(resolved.text()).isEqualTo("Java Тверь");
        assertThat(resolved.area()).isNull();
        assertThat(resolved.fallbackLocation()).isEqualTo("Тверь");
        assertThat(HhLocationResolver.matchesFallback("Тверь, Центральный район", resolved.fallbackLocation()))
                .isTrue();
        assertThat(HhLocationResolver.matchesFallback("Москва", resolved.fallbackLocation())).isFalse();
    }
}
