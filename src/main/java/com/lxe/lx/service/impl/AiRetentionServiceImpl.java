package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiAuditLogMapper;
import com.lxe.lx.mapper.AiConversationMapper;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiEvidenceMapper;
import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.mapper.AiModelCallLogMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiConversation;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiRetentionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiRetentionServiceImpl implements AiRetentionService {

    private final AiMessageMapper messageMapper;
    private final AiEvidenceMapper evidenceMapper;
    private final AiEventMapper eventMapper;
    private final AiAuditLogMapper auditLogMapper;
    private final AiModelCallLogMapper modelCallLogMapper;
    private final AiTaskMapper taskMapper;
    private final AiConversationMapper conversationMapper;

    public AiRetentionServiceImpl(AiMessageMapper messageMapper,
                                  AiEvidenceMapper evidenceMapper,
                                  AiEventMapper eventMapper,
                                  AiAuditLogMapper auditLogMapper,
                                  AiModelCallLogMapper modelCallLogMapper,
                                  AiTaskMapper taskMapper,
                                  AiConversationMapper conversationMapper) {
        this.messageMapper = messageMapper;
        this.evidenceMapper = evidenceMapper;
        this.eventMapper = eventMapper;
        this.auditLogMapper = auditLogMapper;
        this.modelCallLogMapper = modelCallLogMapper;
        this.taskMapper = taskMapper;
        this.conversationMapper = conversationMapper;
    }

    @Override
    public PurgeResult purgeOlderThan(LocalDateTime cutoff) {
        if (cutoff == null) {
            return new PurgeResult(0, 0, 0, 0, 0, 0);
        }
        int messages = messageMapper.deleteOlderThan(cutoff);
        int evidences = evidenceMapper.deleteOlderThan(cutoff);
        int events = eventMapper.deleteOlderThan(cutoff);
        int auditLogs = auditLogMapper.deleteOlderThan(cutoff);
        int modelCallLogs = modelCallLogMapper.deleteOlderThan(cutoff);
        return new PurgeResult(messages, evidences, events, auditLogs, modelCallLogs, 0);
    }

    @Override
    public PurgeResult deleteByConversation(String userId, String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return new PurgeResult(0, 0, 0, 0, 0, 0);
        }
        AiConversation owned = conversationMapper.findByIdAndUser(conversationId, userId);
        if (owned == null) {
            return new PurgeResult(0, 0, 0, 0, 0, 0);
        }
        int evidences = evidenceMapper.deleteByConversation(conversationId);
        int messages = messageMapper.deleteByConversation(conversationId);
        int tasks = taskMapper.deleteByConversation(conversationId);
        return new PurgeResult(messages, evidences, 0, 0, 0, tasks);
    }

    @Override
    public PurgeResult deleteByTask(String userId, String taskId) {
        if (taskId == null || taskId.isEmpty()) {
            return new PurgeResult(0, 0, 0, 0, 0, 0);
        }
        AiTask owned = taskMapper.findByIdAndUser(taskId, userId);
        if (owned == null) {
            return new PurgeResult(0, 0, 0, 0, 0, 0);
        }
        int events = eventMapper.deleteByTask(taskId);
        int tasks = taskMapper.deleteByIdAndUser(taskId, userId);
        return new PurgeResult(0, 0, events, 0, 0, tasks);
    }
}
