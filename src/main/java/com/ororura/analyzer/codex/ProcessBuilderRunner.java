package com.ororura.analyzer.codex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
public class ProcessBuilderRunner implements ProcessRunner {

    private static final int MAX_DIAGNOSTIC_CHARS = 8192;
    private static final List<String> ALLOWED_ENVIRONMENT = List.of(
            "PATH", "HOME", "CODEX_HOME", "OPENAI_API_KEY",
            "HTTP_PROXY", "HTTPS_PROXY", "ALL_PROXY", "NO_PROXY",
            "http_proxy", "https_proxy", "all_proxy", "no_proxy",
            "SSL_CERT_FILE", "SSL_CERT_DIR", "TMPDIR", "TEMP", "TMP",
            "LANG", "LC_ALL", "USER", "LOGNAME", "USERPROFILE",
            "APPDATA", "LOCALAPPDATA", "SYSTEMROOT", "SystemRoot", "PATHEXT");
    private static final long TERMINATION_GRACE_SECONDS = 1;

    private final ProcessStarter processStarter;

    public ProcessBuilderRunner() {
        this(ProcessBuilder::start);
    }

    ProcessBuilderRunner(ProcessStarter processStarter) {
        this.processStarter = processStarter;
    }

    @Override
    public ProcessResult run(ProcessRequest request) throws StartException, ExecutionException {
        Path stdoutPath = request.workingDirectory().resolve("stdout.log");
        Path stderrPath = request.workingDirectory().resolve("stderr.log");
        ProcessBuilder builder = new ProcessBuilder(request.command())
                .directory(request.workingDirectory().toFile())
                .redirectOutput(stdoutPath.toFile())
                .redirectError(stderrPath.toFile());
        restrictEnvironment(builder.environment());

        Process process;
        try {
            process = processStarter.start(builder);
        } catch (IOException exception) {
            throw new StartException(exception);
        }

        try {
            try (var stdin = process.getOutputStream()) {
                stdin.write(request.stdin().getBytes(StandardCharsets.UTF_8));
            }
            boolean completed = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                terminate(process);
                return ProcessResult.timedOut(readDiagnostic(stdoutPath), readDiagnostic(stderrPath));
            }
            return ProcessResult.completed(process.exitValue(), readDiagnostic(stdoutPath), readDiagnostic(stderrPath));
        } catch (InterruptedException exception) {
            terminate(process);
            Thread.currentThread().interrupt();
            throw new ExecutionException("Process execution was interrupted", exception);
        } catch (IOException exception) {
            terminate(process);
            throw new ExecutionException("Process I/O failed", exception);
        }
    }

    private static void restrictEnvironment(Map<String, String> environment) {
        Map<String, String> source = System.getenv();
        environment.clear();
        for (String name : ALLOWED_ENVIRONMENT) {
            String value = source.get(name);
            if (value != null) environment.put(name, value);
        }
    }

    private static void terminate(Process process) {
        if (!process.isAlive()) return;
        process.destroy();
        try {
            if (!process.waitFor(TERMINATION_GRACE_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(TERMINATION_GRACE_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private static String readDiagnostic(Path path) throws IOException {
        if (!Files.exists(path)) return "";
        String value = Files.readString(path, StandardCharsets.UTF_8);
        return value.length() <= MAX_DIAGNOSTIC_CHARS ? value : value.substring(0, MAX_DIAGNOSTIC_CHARS);
    }

    @FunctionalInterface
    interface ProcessStarter {
        Process start(ProcessBuilder builder) throws IOException;
    }
}
