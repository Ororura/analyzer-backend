package com.ororura.analyzer.resume.error;

import org.springframework.http.HttpStatus;

public enum ResumeErrorCode {
    INVALID_FILE(HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    PDF_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
    PDF_PARSE_FAILED(HttpStatus.UNPROCESSABLE_CONTENT),
    EMPTY_RESUME(HttpStatus.UNPROCESSABLE_CONTENT),
    AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    AI_RATE_LIMITED(HttpStatus.SERVICE_UNAVAILABLE),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT),
    AI_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY),
    ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ResumeErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
