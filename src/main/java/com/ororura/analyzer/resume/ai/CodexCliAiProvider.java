package com.ororura.analyzer.resume.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import com.ororura.analyzer.codex.CodexCliAvailabilityChecker;
import com.ororura.analyzer.codex.CodexCliProperties;
import com.ororura.analyzer.codex.ProcessRunner;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CodexCliAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(CodexCliAiProvider.class);
    private static final long MAX_RESULT_BYTES = 1024 * 1024;

    private final CodexCliProperties properties;
    private final CodexCliAvailabilityChecker availabilityChecker;
    private final ProcessRunner processRunner;
    private final ResumeAnalysisPromptFactory promptFactory;
    private final LlmResponseParser parser;
    private final ObjectMapper objectMapper;
    private final Semaphore permits;

    public CodexCliAiProvider(CodexCliProperties properties, CodexCliAvailabilityChecker availabilityChecker,
            ProcessRunner processRunner, ResumeAnalysisPromptFactory promptFactory, LlmResponseParser parser,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.availabilityChecker = availabilityChecker;
        this.processRunner = processRunner;
        this.promptFactory = promptFactory;
        this.parser = parser;
        this.objectMapper = objectMapper;
        this.permits = new Semaphore(properties.maxConcurrentProcesses(), true);
    }

    @Override
    public AiProviderType type() {
        return AiProviderType.CODEX_CLI;
    }

    @Override
    public boolean isAvailable() {
        return availabilityChecker.isAvailable();
    }

    @Override
    public Optional<String> model() {
        return Optional.ofNullable(properties.model()).map(String::trim).filter(value -> !value.isEmpty());
    }

    @Override
    public LlmResumeAnalysisResponse analyze(String resumeText, VacancyMarketData market) {
        if (!permits.tryAcquire()) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "AI provider is unavailable");
        }

        Path directory = null;
        Instant started = Instant.now();
        try {
            directory = Files.createTempDirectory("codex-resume-analysis-");
            Path schemaPath = Files.createTempFile(directory, "resume-analysis-schema-", ".json");
            Path resultPath = Files.createTempFile(directory, "resume-analysis-result-", ".json");
            var prompt = promptFactory.create(resumeText, market);
            Files.writeString(schemaPath, objectMapper.writeValueAsString(prompt.schema()), StandardCharsets.UTF_8);

            List<String> command = command(schemaPath, resultPath);
            log.info("Codex analysis started provider={}", type());
            ProcessRunner.ProcessResult process = processRunner.run(new ProcessRunner.ProcessRequest(
                    command, directory, prompt.cliPrompt(), properties.timeout()));
            if (process.timedOut()) {
                log.warn("Codex process timeout provider={} durationMs={}", type(), elapsedMillis(started));
                throw new ResumeAnalysisException(ResumeErrorCode.AI_TIMEOUT, "AI provider timed out");
            }
            if (process.exitCode() != 0) {
                log.warn("Codex process failed provider={} exitCode={} durationMs={}",
                        type(), process.exitCode(), elapsedMillis(started));
                throw new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_FAILED, "AI provider failed");
            }
            if (Files.size(resultPath) > MAX_RESULT_BYTES) {
                throw new ResumeAnalysisException(ResumeErrorCode.AI_INVALID_RESPONSE,
                        "AI response does not match the required schema");
            }
            LlmResumeAnalysisResponse response = parser.parse(Files.readString(resultPath, StandardCharsets.UTF_8));
            log.info("Codex process completed provider={} exitCode=0 durationMs={}", type(), elapsedMillis(started));
            return response;
        } catch (ResumeAnalysisException exception) {
            throw exception;
        } catch (ProcessRunner.StartException exception) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROCESS_START_FAILED,
                    "AI process could not be started", exception);
        } catch (ProcessRunner.ExecutionException | IOException exception) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_FAILED, "AI provider failed", exception);
        } finally {
            CodexCliAvailabilityChecker.deleteRecursively(directory);
            permits.release();
        }
    }

    private List<String> command(Path schemaPath, Path resultPath) {
        List<String> command = new ArrayList<>(List.of(
                properties.executable(), "exec",
                "--ignore-user-config", "--ignore-rules", "--ephemeral", "--skip-git-repo-check",
                "--sandbox", "read-only",
                "--disable", "shell_tool", "--disable", "unified_exec", "--disable", "multi_agent",
                "--disable", "apps", "--disable", "plugins", "--disable", "hooks",
                "--disable", "browser_use", "--disable", "computer_use", "--disable", "image_generation",
                "--disable", "skill_search",
                "-c", "web_search=\"disabled\"", "-c", "tools.web_search=false",
                "-c", "tools.view_image=false", "-c", "approval_policy=\"never\""));
        model().ifPresent(model -> {
            command.add("--model");
            command.add(model);
        });
        command.addAll(List.of(
                "--output-schema", schemaPath.toString(),
                "--output-last-message", resultPath.toString(),
                "--color", "never", "-"));
        return List.copyOf(command);
    }

    private static long elapsedMillis(Instant started) {
        return Duration.between(started, Instant.now()).toMillis();
    }
}
