package com.lxe.lx.pojo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiMessage {
    private String id;
    private String conversationId;
    private String taskId;
    private String role;
    private String content;
    private String status;
    private String difyMessageId;
    private LocalDateTime createdAt;
}
