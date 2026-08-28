package com.ororura.analyzer.resume.error;

public class ResumeAnalysisException extends RuntimeException {

    private final ResumeErrorCode code;

    public ResumeAnalysisException(ResumeErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ResumeAnalysisException(ResumeErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ResumeErrorCode getCode() {
        return code;
    }
}
