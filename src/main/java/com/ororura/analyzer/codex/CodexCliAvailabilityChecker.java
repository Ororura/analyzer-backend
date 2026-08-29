package com.ororura.analyzer.codex;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CodexCliAvailabilityChecker {

    private static final Logger log = LoggerFactory.getLogger(CodexCliAvailabilityChecker.class);
    private static final List<String> REQUIRED_FLAGS = List.of(
            "--output-schema", "--output-last-message", "--sandbox", "--skip-git-repo-check",
            "--ephemeral", "--ignore-user-config");

    private final CodexCliProperties properties;
    private final ProcessRunner processRunner;

    public CodexCliAvailabilityChecker(CodexCliProperties properties, ProcessRunner processRunner) {
        this.properties = properties;
        this.processRunner = processRunner;
    }

    public boolean isAvailable() {
        if (!properties.enabled()) return false;
        try (var directory = TemporaryDirectory.create("codex-availability-")) {
            ProcessRunner.ProcessResult result = processRunner.run(new ProcessRunner.ProcessRequest(
                    List.of(properties.executable(), "exec", "--help"), directory.path(), "",
                    properties.availabilityTimeout()));
            if (result.timedOut() || result.exitCode() != 0) return false;
            String help = result.stdout() + '\n' + result.stderr();
            return REQUIRED_FLAGS.stream().allMatch(help::contains);
        } catch (IOException | ProcessRunner.StartException | ProcessRunner.ExecutionException exception) {
            log.info("Codex provider unavailable category={}", exception.getClass().getSimpleName());
            return false;
        }
    }
}
