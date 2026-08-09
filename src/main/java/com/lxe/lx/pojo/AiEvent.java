package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiEvent {
    private String id;
    private String taskId;
    private String subtaskId;
    private Long sequence;
    private String eventType;
    private String status;
    private Integer payloadVersion;
    private String payloadJson;
    private String sourceEventId;
    private LocalDateTime occurredAt;
}
