package com.ororura.analyzer.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureBoundaryTests {

    private static final Path SOURCES = Path.of("src/main/java/com/ororura/analyzer");

    @Test
    void domainAndSharedAnalysisStayFrameworkIndependent() throws IOException {
        assertNoImports(path -> path.toString().contains("/domain/")
                        || path.toString().contains("/analysis/"),
                "org.springframework", "jakarta.persistence", ".api.", ".infrastructure.", ".integration.");
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() throws IOException {
        assertNoImports(path -> path.toString().contains("/application/"),
                ".infrastructure.", "jakarta.persistence", "org.springframework.data");
    }

    @Test
    void controllersUseNeitherRepositoriesNorProviders() throws IOException {
        assertNoImports(path -> path.toString().contains("/api/") && path.getFileName().toString().contains("Controller"),
                ".infrastructure.", ".integration.", "Repository");
    }

    @Test
    void featureDependenciesRemainAcyclic() throws IOException {
        assertNoImports(path -> path.toString().contains("/vacancy/"), ".market.", ".resume.");
        assertNoImports(path -> path.toString().contains("/market/"), ".resume.");
    }

    private static void assertNoImports(Predicate<Path> selected, String... forbidden) throws IOException {
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).filter(selected).toList()) {
                for (String line : Files.readAllLines(file)) {
                    if (!line.startsWith("import ")) {
                        continue;
                    }
                    for (String value : forbidden) {
                        if (line.contains(value)) {
                            violations.add(SOURCES.relativize(file) + ": " + line.trim());
                        }
                    }
                }
            }
        }
        assertThat(violations).isEmpty();
    }
}
