package com.lxe.lx.service;

public class AiTaskApiException extends RuntimeException {
    private final int httpStatus;

    public AiTaskApiException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
