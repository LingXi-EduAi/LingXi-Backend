package com.lxe.lx.service.impl;

import com.lxe.lx.domain.dto.AiConversationMessage;
import com.lxe.lx.domain.dto.AiConversationMessagePage;
import com.lxe.lx.domain.dto.AiConversationPage;
import com.lxe.lx.domain.dto.AiConversationSummary;
import com.lxe.lx.mapper.AiConversationMapper;
import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiConversation;
import com.lxe.lx.pojo.AiEvidence;
import com.lxe.lx.pojo.AiMessage;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiConversationService;
import com.lxe.lx.service.AiEvidenceService;
import com.lxe.lx.service.AiMessageService;
import com.lxe.lx.service.AiTaskApiException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiConversationServiceImpl implements AiConversationService {
    private static final String ACTIVE = "ACTIVE";
    private static final String DELETED = "DELETED";

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiMessageService messageService;
    private final AiEvidenceService evidenceService;
    private final AiTaskMapper taskMapper;

    public AiConversationServiceImpl(
            AiConversationMapper conversationMapper,
            AiMessageMapper messageMapper,
            AiMessageService messageService,
            AiEvidenceService evidenceService,
            AiTaskMapper taskMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.messageService = messageService;
        this.evidenceService = evidenceService;
        this.taskMapper = taskMapper;
    }

    @Override
    public AiConversation createIfAbsent(String conversationId, String userId, String title) {
        AiConversation existing = conversationMapper.findByIdAndUser(conversationId, userId);
        if (existing != null) {
            if (DELETED.equals(existing.getState())) {
                throw new AiTaskApiException(404, "会话不存在");
            }
            return existing;
        }
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        conversation.setTitle(normalizeTitle(title));
        conversation.setState(ACTIVE);
        conversation.setVersion(1);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(conversation.getCreatedAt());
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public AiConversation requireActiveOwned(String conversationId, String userId) {
        AiConversation conversation = conversationMapper.findByIdAndUser(conversationId, userId);
        if (conversation == null || !ACTIVE.equals(conversation.getState())) {
            throw new AiTaskApiException(404, "会话不存在");
        }
        return conversation;
    }

    @Override
    public AiConversationPage list(String userId, int page, int size) {
        validatePage(page, size);
        List<AiConversation> conversations = conversationMapper.findByUser(
                userId, ACTIVE, (page - 1) * size, size);
        AiConversationPage result = new AiConversationPage();
        result.setCurrentPage(page);
        result.setPageSize(size);
        result.setTotal(conversationMapper.countByUser(userId, ACTIVE));
        for (AiConversation conversation : conversations) {
            AiConversationSummary summary = new AiConversationSummary();
            summary.setConversationId(conversation.getId());
            summary.setTitle(conversation.getTitle());
            summary.setState(conversation.getState());
            summary.setCreatedAt(conversation.getCreatedAt());
            summary.setUpdatedAt(conversation.getUpdatedAt());
            List<AiMessage> latest = messageMapper.findByConversation(conversation.getId(), 0, 1);
            if (!latest.isEmpty()) {
                summary.setLastMessage(latest.get(0).getContent());
                summary.setLastMessageRole(latest.get(0).getRole());
                AiTask task = taskMapper.findLatestByConversationId(conversation.getId());
                if (task != null) {
                    summary.setLastTaskStatus(task.getStatus());
                }
            }
            result.getList().add(summary);
        }
        return result;
    }

    @Override
    public AiConversationMessagePage messages(String conversationId, String userId, int page, int size) {
        requireActiveOwned(conversationId, userId);
        validatePage(page, size);
        List<AiMessage> messages = messageMapper.findByConversation(
                conversationId, (page - 1) * size, size);
        AiConversationMessagePage result = new AiConversationMessagePage();
        result.setCurrentPage(page);
        result.setPageSize(size);
        result.setTotal(messageService.countByConversation(conversationId));
        for (AiMessage message : messages) {
            AiConversationMessage view = new AiConversationMessage();
            view.setId(message.getId());
            view.setConversationId(message.getConversationId());
            view.setTaskId(message.getTaskId());
            view.setRole(message.getRole());
            view.setContent(message.getContent());
            view.setStatus(message.getStatus());
            view.setDifyMessageId(message.getDifyMessageId());
            view.setCreatedAt(message.getCreatedAt());
            List<AiEvidence> evidence = evidenceService.getByMessageId(message.getId());
            view.setEvidence(evidence);
            result.getList().add(view);
        }
        java.util.Collections.reverse(result.getList());
        return result;
    }

    @Override
    @Transactional
    public AiConversation rename(String conversationId, String userId, String title) {
        AiConversation conversation = requireActiveOwned(conversationId, userId);
        conversation.setTitle(normalizeTitle(title));
        conversation.setUpdatedAt(LocalDateTime.now());
        if (conversationMapper.updateTitle(conversation) != 1) {
            throw new AiTaskApiException(409, "会话已被其他请求修改，请刷新后重试");
        }
        return conversationMapper.findByIdAndUser(conversationId, userId);
    }

    @Override
    @Transactional
    public void delete(String conversationId, String userId) {
        AiConversation conversation = conversationMapper.findByIdAndUser(conversationId, userId);
        if (conversation == null) {
            throw new AiTaskApiException(404, "会话不存在");
        }
        if (DELETED.equals(conversation.getState())) {
            return;
        }
        conversation.setDeletedAt(LocalDateTime.now());
        conversation.setUpdatedAt(conversation.getDeletedAt());
        if (conversationMapper.softDelete(conversation) != 1) {
            throw new AiTaskApiException(409, "会话已被其他请求修改，请刷新后重试");
        }
    }

    private String normalizeTitle(String title) {
        String normalized = StringUtils.trimToEmpty(title);
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        return StringUtils.defaultIfBlank(normalized, "AI 会话");
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new AiTaskApiException(400, "分页参数无效");
        }
    }
}
