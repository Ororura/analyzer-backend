package com.ororura.analyzer.integration.codex;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class CodexCommandFactory {

    private final CodexCliProperties properties;

    public CodexCommandFactory(CodexCliProperties properties) {
        this.properties = properties;
    }

    public List<String> create(Path schemaPath, Path resultPath, Optional<String> model) {
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
        model.ifPresent(value -> {
            command.add("--model");
            command.add(value);
        });
        command.addAll(List.of(
                "--output-schema", schemaPath.toString(),
                "--output-last-message", resultPath.toString(),
                "--color", "never", "-"));
        return List.copyOf(command);
    }
}
