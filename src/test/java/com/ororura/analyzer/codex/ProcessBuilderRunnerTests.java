package com.ororura.analyzer.codex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessBuilderRunnerTests {

    @TempDir
    Path directory;

    @Test
    void writesUtf8PromptAndConfiguresRedirects() throws Exception {
        FakeProcess process = new FakeProcess(true, 0);
        ProcessBuilderRunner runner = new ProcessBuilderRunner(builder -> {
            assertThat(builder.command()).containsExactly("codex", "exec", "-");
            assertThat(builder.directory()).isEqualTo(directory.toFile());
            assertThat(builder.redirectOutput().file().toPath()).isEqualTo(directory.resolve("stdout.log"));
            assertThat(builder.redirectError().file().toPath()).isEqualTo(directory.resolve("stderr.log"));
            return process;
        });

        ProcessRunner.ProcessResult result = runner.run(new ProcessRunner.ProcessRequest(
                List.of("codex", "exec", "-"), directory, "резюме", Duration.ofSeconds(1)));

        assertThat(result.exitCode()).isZero();
        assertThat(process.stdin.toString(StandardCharsets.UTF_8)).isEqualTo("резюме");
    }

    @Test
    void destroysAndForciblyReapsTimedOutProcess() throws Exception {
        FakeProcess process = new FakeProcess(false, 0);
        ProcessBuilderRunner runner = new ProcessBuilderRunner(builder -> process);

        ProcessRunner.ProcessResult result = runner.run(new ProcessRunner.ProcessRequest(
                List.of("codex", "exec"), directory, "prompt", Duration.ofMillis(1)));

        assertThat(result.timedOut()).isTrue();
        assertThat(process.destroyed).isTrue();
        assertThat(process.destroyedForcibly).isTrue();
        assertThat(process.isAlive()).isFalse();
    }

    @Test
    void distinguishesProcessStartFailure() {
        ProcessBuilderRunner runner = new ProcessBuilderRunner(builder -> {
            throw new IOException("missing executable");
        });

        assertThatThrownBy(() -> runner.run(new ProcessRunner.ProcessRequest(
                List.of("missing"), directory, "", Duration.ofSeconds(1))))
                .isInstanceOf(ProcessRunner.StartException.class);
    }

    private static final class FakeProcess extends Process {
        private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        private final boolean completesNormally;
        private final int exitCode;
        private boolean alive = true;
        private boolean destroyed;
        private boolean destroyedForcibly;

        private FakeProcess(boolean completesNormally, int exitCode) {
            this.completesNormally = completesNormally;
            this.exitCode = exitCode;
        }

        @Override public OutputStream getOutputStream() { return stdin; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
        @Override public int waitFor() { alive = false; return exitCode; }
        @Override public boolean waitFor(long timeout, TimeUnit unit) {
            if (completesNormally || destroyedForcibly) alive = false;
            return completesNormally || destroyedForcibly;
        }
        @Override public int exitValue() { return exitCode; }
        @Override public void destroy() { destroyed = true; }
        @Override public Process destroyForcibly() { destroyedForcibly = true; alive = false; return this; }
        @Override public boolean isAlive() { return alive; }
    }
}
