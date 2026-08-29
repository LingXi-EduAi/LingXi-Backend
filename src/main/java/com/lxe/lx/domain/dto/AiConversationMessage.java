package com.lxe.lx.domain.dto;

import com.lxe.lx.pojo.AiEvidence;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AiConversationMessage {
    private String id;
    private String conversationId;
    private String taskId;
    private String role;
    private String content;
    private String status;
    private String difyMessageId;
    private String attachments;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private List<AiEvidence> evidence = new ArrayList<>();
}
