package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiSubtask {
    private String id;
    private String taskId;
    private String parentId;
    private String agentType;
    private String goal;
    private String inputsJson;
    private String dependencyJson;
    private String status;
    private Integer executionNo;
    private Integer retryCount;
    private String errorCode;
    private String errorMessage;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime updatedAt;
}
