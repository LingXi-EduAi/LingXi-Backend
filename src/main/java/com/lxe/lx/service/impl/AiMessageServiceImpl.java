package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.pojo.AiMessage;
import com.lxe.lx.service.AiMessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AiMessageServiceImpl implements AiMessageService {
    private final AiMessageMapper messageMapper;

    public AiMessageServiceImpl(AiMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public AiMessage saveUserQuestion(String conversationId, String taskId, String content) {
        return saveUserQuestion(conversationId, taskId, content, null);
    }

    @Override
    public AiMessage saveUserQuestion(String conversationId, String taskId, String content,
                                      String attachments) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        AiMessage existing = messageMapper.findByTaskAndRole(taskId, "user");
        if (existing != null) {
            return existing;
        }
        AiMessage message = newMessage(conversationId, taskId, "user", content, null);
        message.setAttachments(attachments);
        messageMapper.insert(message);
        return message;
    }

    @Override
    public AiMessage saveAssistantAnswer(
            String conversationId,
            String taskId,
            String content,
            String difyMessageId) {
        return saveAssistantAnswer(conversationId, taskId, content, difyMessageId, null);
    }

    @Override
    public AiMessage saveAssistantAnswer(
            String conversationId,
            String taskId,
            String content,
            String difyMessageId,
            String attachments) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        AiMessage existing = messageMapper.findByTaskAndRole(taskId, "assistant");
        if (existing != null) {
            return existing;
        }
        if (StringUtils.isNotBlank(difyMessageId)) {
            existing = messageMapper.findByDifyMessageId(difyMessageId);
            if (existing != null) {
                return existing;
            }
        }
        AiMessage message = newMessage(conversationId, taskId, "assistant", content, difyMessageId);
        message.setAttachments(attachments);
        messageMapper.insert(message);
        return message;
    }

    @Override
    public AiMessage saveAssistantError(
            String conversationId,
            String taskId,
            String errorCode,
            String errorMessage,
            String difyMessageId) {
        AiMessage existing = messageMapper.findByTaskAndRole(taskId, "assistant");
        if (existing != null) {
            return existing;
        }
        AiMessage message = newMessage(conversationId, taskId, "assistant",
                StringUtils.defaultString(errorMessage), difyMessageId);
        message.setStatus("FAILED");
        message.setErrorCode(errorCode);
        message.setErrorMessage(errorMessage);
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<AiMessage> getMessagesByConversation(String conversationId, int page, int size) {
        if (StringUtils.isBlank(conversationId) || page < 1 || size < 1) {
            return Collections.emptyList();
        }
        return messageMapper.findByConversation(conversationId, (page - 1) * size, size);
    }

    @Override
    public int countByConversation(String conversationId) {
        return messageMapper.countByConversation(conversationId);
    }

    private AiMessage newMessage(
            String conversationId,
            String taskId,
            String role,
            String content,
            String difyMessageId) {
        AiMessage message = new AiMessage();
        message.setId(UUID.randomUUID().toString().replace("-", ""));
        message.setConversationId(conversationId);
        message.setTaskId(taskId);
        message.setRole(role);
        message.setContent(content);
        message.setStatus("COMPLETED");
        message.setDifyMessageId(difyMessageId);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
