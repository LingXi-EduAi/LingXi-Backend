package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiSubtaskSnapshot {
    private String subtaskId;
    private String parentId;
    private String agentType;
    private String goal;
    private String status;
    private Integer executionNo;
    private Integer retryCount;
    private String errorCode;
    private String errorMessage;
}
