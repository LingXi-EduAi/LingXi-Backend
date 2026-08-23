package com.lxe.lx.service;

import com.lxe.lx.pojo.AiMessage;

import java.util.List;

public interface AiMessageService {
    AiMessage saveUserQuestion(String conversationId, String taskId, String content);

    AiMessage saveAssistantAnswer(
            String conversationId,
            String taskId,
            String content,
            String difyMessageId
    );

    List<AiMessage> getMessagesByConversation(String conversationId, int page, int size);

    int countByConversation(String conversationId);
}
