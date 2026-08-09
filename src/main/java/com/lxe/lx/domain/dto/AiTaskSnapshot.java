package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AiTaskSnapshot {
    private String taskId;
    private String conversationId;
    private String taskType;
    private String status;
    private Integer progress;
    private Object result;
    private String errorCode;
    private String errorMessage;
    private List<AiSubtaskSnapshot> subtasks = new ArrayList<>();
}
