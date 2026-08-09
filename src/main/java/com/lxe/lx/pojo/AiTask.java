package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiTask {
    private String id;
    private String userId;
    private String conversationId;
    private String taskType;
    private String status;
    private Integer progress;
    private String requestJson;
    private String resultJson;
    private String errorCode;
    private String errorMessage;
    private String difyTaskId;
    private String difyConversationId;
    private Long eventSequence;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime updatedAt;
}
