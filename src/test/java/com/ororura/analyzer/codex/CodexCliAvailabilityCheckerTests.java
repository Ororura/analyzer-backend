package com.ororura.analyzer.codex;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

class CodexCliAvailabilityCheckerTests {

    private static final String COMPATIBLE_HELP = """
            --output-schema --output-last-message --sandbox --skip-git-repo-check
            --ephemeral --ignore-user-config
            """;

    @Test
    void reportsCompatibleCliAndCleansTemporaryDirectory() throws Exception {
        ProcessRunner runner = mock(ProcessRunner.class);
        AtomicReference<Path> directory = new AtomicReference<>();
        when(runner.run(any())).thenAnswer(invocation -> {
            ProcessRunner.ProcessRequest request = invocation.getArgument(0);
            directory.set(request.workingDirectory());
            return ProcessRunner.ProcessResult.completed(0, COMPATIBLE_HELP, "");
        });

        assertThat(new CodexCliAvailabilityChecker(properties(true), runner).isAvailable()).isTrue();
        assertThat(directory.get()).doesNotExist();
    }

    @Test
    void rejectsDisabledMissingTimedOutAndIncompatibleCli() throws Exception {
        ProcessRunner runner = mock(ProcessRunner.class);
        assertThat(new CodexCliAvailabilityChecker(properties(false), runner).isAvailable()).isFalse();
        verify(runner, never()).run(any());

        when(runner.run(any())).thenThrow(new ProcessRunner.StartException(new IOException("missing")));
        assertThat(new CodexCliAvailabilityChecker(properties(true), runner).isAvailable()).isFalse();

        reset(runner);
        when(runner.run(any())).thenReturn(ProcessRunner.ProcessResult.timedOut("", ""));
        assertThat(new CodexCliAvailabilityChecker(properties(true), runner).isAvailable()).isFalse();

        reset(runner);
        when(runner.run(any())).thenReturn(ProcessRunner.ProcessResult.completed(0, "old cli", ""));
        assertThat(new CodexCliAvailabilityChecker(properties(true), runner).isAvailable()).isFalse();
    }

    private static CodexCliProperties properties(boolean enabled) {
        return new CodexCliProperties(enabled, "codex", "", Duration.ofSeconds(120),
                Duration.ofSeconds(5), 2);
    }
}
