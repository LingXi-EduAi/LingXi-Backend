package com.lxe.lx.service;

import com.lxe.lx.pojo.AiMessage;

import java.util.List;

public interface AiMessageService {
    AiMessage saveUserQuestion(String conversationId, String taskId, String content);

    AiMessage saveUserQuestion(String conversationId, String taskId, String content, String attachments);

    AiMessage saveAssistantAnswer(
            String conversationId,
            String taskId,
            String content,
            String difyMessageId
    );

    AiMessage saveAssistantAnswer(
            String conversationId,
            String taskId,
            String content,
            String difyMessageId,
            String attachments
    );

    AiMessage saveAssistantError(
            String conversationId,
            String taskId,
            String errorCode,
            String errorMessage,
            String difyMessageId
    );

    List<AiMessage> getMessagesByConversation(String conversationId, int page, int size);

    int countByConversation(String conversationId);
}
