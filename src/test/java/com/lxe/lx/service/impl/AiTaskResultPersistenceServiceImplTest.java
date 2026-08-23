package com.lxe.lx.service.impl;

import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.pojo.AiEvidence;
import com.lxe.lx.pojo.AiEvent;
import com.lxe.lx.pojo.AiMessage;
import com.lxe.lx.service.AiEventService;
import com.lxe.lx.service.AiEvidenceService;
import com.lxe.lx.service.AiMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskResultPersistenceServiceImplTest {
    private AiEventService eventService;
    private AiMessageService messageService;
    private AiEvidenceService evidenceService;
    private AiTaskResultPersistenceServiceImpl service;

    @BeforeEach
    void setUp() {
        eventService = mock(AiEventService.class);
        messageService = mock(AiMessageService.class);
        evidenceService = mock(AiEvidenceService.class);
        service = new AiTaskResultPersistenceServiceImpl(eventService, messageService, evidenceService);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void writesAnswerAndEvidenceAfterTerminalEventRecord() {
        AiMessage message = new AiMessage();
        message.setId("message-1");
        when(eventService.record(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new AiEvent());
        when(messageService.saveAssistantAnswer(
                "conversation-1", "task-1", "answer", "dify-message-1"))
                .thenReturn(message);

        service.recordTerminalEvent(
                "task-1", "subtask-1", event(), "source-1", "{}", null, null,
                "answer", "dify-message-1", Collections.singletonList(new AiEvidence()));

        verify(messageService).saveAssistantAnswer(
                "conversation-1", "task-1", "answer", "dify-message-1");
        verify(evidenceService).saveAll(eq("message-1"), any());
    }

    @Test
    void skipsMessageWhenEventWasDuplicateOrIgnored() {
        when(eventService.record(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        service.recordTerminalEvent(
                "task-1", "subtask-1", event(), "source-1", "{}", null, null,
                "answer", "dify-message-1", Collections.emptyList());

        verify(messageService, org.mockito.Mockito.never())
                .saveAssistantAnswer(any(), any(), any(), any());
    }

    private LingXiEvent event() {
        LingXiEvent event = new LingXiEvent();
        event.setConversationId("conversation-1");
        event.setEventType("task_finished");
        event.setStatus("SUCCEEDED");
        return event;
    }
}
