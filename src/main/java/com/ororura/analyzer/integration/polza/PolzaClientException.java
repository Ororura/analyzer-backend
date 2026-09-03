package com.ororura.analyzer.integration.polza;

public class PolzaClientException extends RuntimeException {

    private final Integer status;

    public PolzaClientException(String message, Integer status) {
        super(message);
        this.status = status;
    }

    public PolzaClientException(String message, Integer status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}
