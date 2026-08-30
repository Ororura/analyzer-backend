package com.ororura.analyzer.resume.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import com.ororura.analyzer.codex.CodexCliAvailabilityChecker;
import com.ororura.analyzer.codex.CodexCommandFactory;
import com.ororura.analyzer.codex.CodexCliProperties;
import com.ororura.analyzer.codex.ProcessRunner;
import com.ororura.analyzer.codex.TemporaryDirectory;
import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import com.ororura.analyzer.resume.market.MarketAnalysisProfile;
import com.ororura.analyzer.vacancy.market.VacancyMarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class CodexCliAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(CodexCliAiProvider.class);
    private static final long MAX_RESULT_BYTES = 1024 * 1024;

    private final CodexCliProperties properties;
    private final CodexCliAvailabilityChecker availabilityChecker;
    private final ProcessRunner processRunner;
    private final CodexCommandFactory commandFactory;
    private final ResumeAnalysisPromptFactory promptFactory;
    private final LlmResponseParser parser;
    private final ObjectMapper objectMapper;
    private final Semaphore permits;

    public CodexCliAiProvider(
            CodexCliProperties properties,
            CodexCliAvailabilityChecker availabilityChecker,
            ProcessRunner processRunner,
            CodexCommandFactory commandFactory,
            ResumeAnalysisPromptFactory promptFactory,
            LlmResponseParser parser,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.availabilityChecker = availabilityChecker;
        this.processRunner = processRunner;
        this.commandFactory = commandFactory;
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
    public LlmResumeAnalysisResponse analyze(MarketAnalysisProfile profile, String resumeText, VacancyMarketData market) {
        acquirePermit();
        try {
            return performAnalysis(profile, resumeText, market);
        } finally {
            permits.release();
        }
    }

    private void acquirePermit() {
        if (!permits.tryAcquire()) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "AI provider is unavailable");
        }
    }

    private LlmResumeAnalysisResponse performAnalysis(MarketAnalysisProfile profile, String resumeText,
            VacancyMarketData market) {
        Instant started = Instant.now();
        try (CodexWorkspace workspace = CodexWorkspace.create()) {
            var prompt = promptFactory.create(profile, resumeText, market);
            writeSchema(workspace.schemaPath(), prompt.schema());
            log.info("Codex analysis started provider={}", type());
            ProcessRunner.ProcessResult process = executeCodex(workspace, prompt.cliPrompt());
            validateProcessResult(process, started);
            LlmResumeAnalysisResponse response = readResponse(workspace.resultPath());
            log.info("Codex process completed provider={} exitCode=0 durationMs={}", type(), elapsedMillis(started));
            return response;
        } catch (ProcessRunner.StartException exception) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROCESS_START_FAILED,
                    "AI process could not be started", exception);
        } catch (ProcessRunner.ExecutionException | IOException exception) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_FAILED, "AI provider failed", exception);
        }
    }

    private void writeSchema(Path schemaPath, JsonNode schema) throws IOException {
        Files.writeString(schemaPath, objectMapper.writeValueAsString(schema), StandardCharsets.UTF_8);
    }

    private ProcessRunner.ProcessResult executeCodex(CodexWorkspace workspace, String prompt)
            throws ProcessRunner.StartException, ProcessRunner.ExecutionException {
        List<String> command = commandFactory.create(workspace.schemaPath(), workspace.resultPath(), model());
        return processRunner.run(new ProcessRunner.ProcessRequest(
                command, workspace.directory(), prompt, properties.timeout()));
    }

    private void validateProcessResult(ProcessRunner.ProcessResult process, Instant started) {
        if (process.timedOut()) {
            log.warn("Codex process timeout provider={} durationMs={}", type(), elapsedMillis(started));
            throw new ResumeAnalysisException(ResumeErrorCode.AI_TIMEOUT, "AI provider timed out");
        }
        if (process.exitCode() != 0) {
            log.warn("Codex process failed provider={} exitCode={} durationMs={}",
                    type(), process.exitCode(), elapsedMillis(started));
            throw new ResumeAnalysisException(ResumeErrorCode.AI_PROVIDER_FAILED, "AI provider failed");
        }
    }

    private LlmResumeAnalysisResponse readResponse(Path resultPath) throws IOException {
        if (Files.size(resultPath) > MAX_RESULT_BYTES) {
            throw new ResumeAnalysisException(ResumeErrorCode.AI_INVALID_RESPONSE,
                    "AI response does not match the required schema");
        }
        return parser.parse(Files.readString(resultPath, StandardCharsets.UTF_8));
    }

    private static long elapsedMillis(Instant started) {
        return Duration.between(started, Instant.now()).toMillis();
    }

    private record CodexWorkspace(TemporaryDirectory temporaryDirectory, Path schemaPath, Path resultPath)
            implements AutoCloseable {

        private static CodexWorkspace create() throws IOException {
            TemporaryDirectory directory = TemporaryDirectory.create("codex-resume-analysis-");
            try {
                Path schemaPath = Files.createTempFile(
                        directory.path(), "resume-analysis-schema-", ".json");
                Path resultPath = Files.createTempFile(
                        directory.path(), "resume-analysis-result-", ".json");
                return new CodexWorkspace(directory, schemaPath, resultPath);
            } catch (IOException exception) {
                directory.close();
                throw exception;
            }
        }

        private Path directory() {
            return temporaryDirectory.path();
        }

        @Override
        public void close() {
            temporaryDirectory.close();
        }
    }
}
