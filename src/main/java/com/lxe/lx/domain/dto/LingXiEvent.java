package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class LingXiEvent {
    public static final int PAYLOAD_VERSION = 1;

    private String eventId;
    private long sequence;
    private String eventType;
    private String taskId;
    private String conversationId;
    private String occurredAt;
    private String status;
    private int payloadVersion = PAYLOAD_VERSION;
    private Map<String, Object> payload = new LinkedHashMap<>();
}
