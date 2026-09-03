package com.ororura.analyzer.integration.codex;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodexCommandFactoryTests {

    private static final Path SCHEMA_PATH = Path.of("schema.json");
    private static final Path RESULT_PATH = Path.of("result.json");

    @Test
    void createsRestrictedStructuredCommandWithModel() {
        var command = factory().create(SCHEMA_PATH, RESULT_PATH, Optional.of("gpt-5.4"));

        assertThat(command).containsExactly(
                "codex", "exec", "--ignore-user-config", "--ignore-rules", "--ephemeral",
                "--skip-git-repo-check", "--sandbox", "read-only",
                "--disable", "shell_tool", "--disable", "unified_exec", "--disable", "multi_agent",
                "--disable", "apps", "--disable", "plugins", "--disable", "hooks",
                "--disable", "browser_use", "--disable", "computer_use", "--disable", "image_generation",
                "--disable", "skill_search",
                "-c", "web_search=\"disabled\"", "-c", "tools.web_search=false",
                "-c", "tools.view_image=false", "-c", "approval_policy=\"never\"",
                "--model", "gpt-5.4",
                "--output-schema", SCHEMA_PATH.toString(),
                "--output-last-message", RESULT_PATH.toString(),
                "--color", "never", "-");
    }

    @Test
    void omitsModelArgumentsWhenModelIsAbsent() {
        var command = factory().create(SCHEMA_PATH, RESULT_PATH, Optional.empty());

        assertThat(command).doesNotContain("--model");
    }

    private static CodexCommandFactory factory() {
        CodexCliProperties properties = new CodexCliProperties(
                true,
                "codex",
                "",
                Duration.ofSeconds(120),
                Duration.ofSeconds(5),
                2);
        return new CodexCommandFactory(properties);
    }
}
