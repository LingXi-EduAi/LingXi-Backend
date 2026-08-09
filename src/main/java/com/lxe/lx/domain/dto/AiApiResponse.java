package com.lxe.lx.domain.dto;

import lombok.Getter;

@Getter
public class AiApiResponse<T> {
    private final int status;
    private final String msg;
    private final String requestId;
    private final T data;

    private AiApiResponse(int status, String msg, String requestId, T data) {
        this.status = status;
        this.msg = msg;
        this.requestId = requestId;
        this.data = data;
    }

    public static <T> AiApiResponse<T> success(String requestId, T data) {
        return new AiApiResponse<>(200, "success", requestId, data);
    }

    public static <T> AiApiResponse<T> error(int status, String message, String requestId) {
        return new AiApiResponse<>(status, message, requestId, null);
    }
}
