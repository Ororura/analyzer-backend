package com.ororura.analyzer.vacancy.hh;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HhTextCleanerTests {

    @ParameterizedTest
    @MethodSource("htmlSamples")
    void cleansHtml(String html, String expected) {
        assertThat(HhTextCleaner.clean(html)).isEqualTo(expected);
    }

    private static Stream<Arguments> htmlSamples() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("   ", null),
                Arguments.of("<p>Первая строка<br>Вторая</p>", "Первая строка\nВторая"),
                Arguments.of("<ul><li>Java</li><li>Spring &amp; Boot</li></ul>", "Java\nSpring & Boot"),
                Arguments.of("<p>&lt;Backend&gt;&nbsp;developer</p>", "<Backend>\u00a0developer"));
    }
}
