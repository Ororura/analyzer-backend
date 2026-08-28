package com.ororura.analyzer.polza;

import java.util.List;

public class AiResponseValidationException extends RuntimeException {

    private final List<String> issues;

    public AiResponseValidationException(String message, List<String> issues) {
        super(message);
        this.issues = List.copyOf(issues);
    }

    public List<String> getIssues() {
        return issues;
    }
}
