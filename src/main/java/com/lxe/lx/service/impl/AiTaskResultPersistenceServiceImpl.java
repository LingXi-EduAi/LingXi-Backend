package com.lxe.lx.service.impl;

import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.pojo.AiEvidence;
import com.lxe.lx.pojo.AiMessage;
import com.lxe.lx.service.AiEventService;
import com.lxe.lx.service.AiEvidenceService;
import com.lxe.lx.service.AiMessageService;
import com.lxe.lx.service.AiTaskResultPersistenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class AiTaskResultPersistenceServiceImpl implements AiTaskResultPersistenceService {
    private final AiEventService eventService;
    private final AiMessageService messageService;
    private final AiEvidenceService evidenceService;

    public AiTaskResultPersistenceServiceImpl(
            AiEventService eventService,
            AiMessageService messageService,
            AiEvidenceService evidenceService) {
        this.eventService = eventService;
        this.messageService = messageService;
        this.evidenceService = evidenceService;
    }

    @Override
    @Transactional
    public void recordTerminalEvent(
            String taskId,
            String subtaskId,
            LingXiEvent event,
            String sourceEventId,
            String resultJson,
            String errorCode,
            String errorMessage,
            String answer,
            String difyMessageId,
            List<AiEvidence> evidences) {
        if (eventService.record(taskId, subtaskId, event, sourceEventId,
                resultJson, errorCode, errorMessage) == null) {
            return;
        }
        AiMessage message = messageService.saveAssistantAnswer(
                event.getConversationId(), taskId, answer, difyMessageId);
        if (message != null) {
            evidenceService.saveAll(message.getId(),
                    evidences == null ? Collections.emptyList() : evidences);
        }
    }
}
