package com.ororura.analyzer.codex;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface ProcessRunner {

    ProcessResult run(ProcessRequest request) throws StartException, ExecutionException;

    record ProcessRequest(List<String> command, Path workingDirectory, String stdin, Duration timeout) {
        public ProcessRequest {
            command = List.copyOf(command);
        }
    }

    record ProcessResult(int exitCode, boolean timedOut, String stdout, String stderr) {

        public static ProcessResult completed(int exitCode, String stdout, String stderr) {
            return new ProcessResult(exitCode, false, stdout, stderr);
        }

        public static ProcessResult timedOut(String stdout, String stderr) {
            return new ProcessResult(-1, true, stdout, stderr);
        }
    }

    final class StartException extends Exception {
        public StartException(Throwable cause) {
            super("Unable to start process", cause);
        }
    }

    final class ExecutionException extends Exception {
        public ExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
