package com.ororura.analyzer.vacancy.hh;

public class VacancySourceException extends RuntimeException {

    private final String code;
    private final int status;
    private final String retryAfter;

    public VacancySourceException(String code, String message, int status, String retryAfter, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}
