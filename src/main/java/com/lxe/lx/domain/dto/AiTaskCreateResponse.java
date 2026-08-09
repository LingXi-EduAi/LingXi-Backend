package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTaskCreateResponse {
    private String taskId;
    private String conversationId;
    private String status;
    private String eventUrl;
}
