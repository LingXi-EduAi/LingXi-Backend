package com.lxe.lx.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AiConversationSummary {
    private String conversationId;
    private String title;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastMessage;
    private String lastMessageRole;
    private String lastTaskStatus;
}
