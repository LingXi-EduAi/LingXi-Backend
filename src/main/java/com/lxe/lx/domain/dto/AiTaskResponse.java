package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTaskResponse {
    private String answer;
    private String conversationId;
    private String messageId;
    private long elapsedMs;
}
