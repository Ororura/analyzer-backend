package com.ororura.analyzer.integration.codex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class TemporaryDirectory implements AutoCloseable {

    private final Path path;

    private TemporaryDirectory(Path path) {
        this.path = path;
    }

    public static TemporaryDirectory create(String prefix) throws IOException {
        return new TemporaryDirectory(Files.createTempDirectory(prefix));
    }

    public Path path() {
        return path;
    }

    @Override
    public void close() {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(TemporaryDirectory::deleteIfExists);
        } catch (IOException ignored) {
            // Cleanup is best-effort and paths are never exposed to callers.
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Cleanup is best-effort and paths are never exposed to callers.
        }
    }
}
