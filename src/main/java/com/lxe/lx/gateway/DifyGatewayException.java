package com.lxe.lx.gateway;

public class DifyGatewayException extends RuntimeException {
    private final Integer httpStatus;
    private final boolean retryable;

    public DifyGatewayException(String message, Integer httpStatus, boolean retryable, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
