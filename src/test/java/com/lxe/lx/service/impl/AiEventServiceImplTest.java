package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.AiTaskStatus;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiEvent;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.TaskEventBroadcaster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiEventServiceImplTest {
    private AiTaskMapper taskMapper;
    private AiEventMapper eventMapper;
    private TaskEventBroadcaster broadcaster;
    private AiEventServiceImpl service;

    @BeforeEach
    void setUp() {
        taskMapper = mock(AiTaskMapper.class);
        eventMapper = mock(AiEventMapper.class);
        broadcaster = mock(TaskEventBroadcaster.class);
        service = new AiEventServiceImpl(
                taskMapper,
                mock(AiSubtaskMapper.class),
                eventMapper,
                broadcaster,
                new ObjectMapper());
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void allocatesSequenceAndBroadcastsOnlyAfterCommit() {
        AiTask task = task(AiTaskStatus.CREATED, 0L);
        when(taskMapper.lockById("task-1")).thenReturn(task);
        when(taskMapper.updateAfterEvent(task)).thenReturn(1);
        LingXiEvent contract = event(LingXiEventType.TASK_STARTED, AiTaskStatus.RUNNING);

        AiEvent stored = service.record(
                "task-1", null, contract, "source-1", null, null, null);

        assertEquals(1L, stored.getSequence());
        assertEquals("task-1:1", contract.getEventId());
        verify(eventMapper).insert(any(AiEvent.class));
        verify(broadcaster, never()).publish(any(LingXiEvent.class));

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(broadcaster).publish(any(LingXiEvent.class));
    }

    @Test
    void returnsExistingEventForDuplicateSourceId() {
        when(taskMapper.lockById("task-1")).thenReturn(task(AiTaskStatus.RUNNING, 3L));
        AiEvent existing = new AiEvent();
        existing.setSequence(2L);
        when(eventMapper.findBySourceEventId("task-1", "source-1")).thenReturn(existing);

        AiEvent result = service.record("task-1", null,
                event(LingXiEventType.NODE_PROGRESS, AiTaskStatus.RUNNING),
                "source-1", null, null, null);

        assertEquals(existing, result);
        verify(eventMapper, never()).insert(any(AiEvent.class));
        verify(taskMapper, never()).updateAfterEvent(any(AiTask.class));
    }

    @Test
    void ignoresLateEventAfterTerminalState() {
        when(taskMapper.lockById("task-1")).thenReturn(task(AiTaskStatus.STOPPED, 4L));

        AiEvent result = service.record("task-1", null,
                event(LingXiEventType.TASK_FINISHED, AiTaskStatus.SUCCEEDED),
                null, "{}", null, null);

        assertNull(result);
        verify(eventMapper, never()).insert(any(AiEvent.class));
        verify(taskMapper, never()).updateAfterEvent(any(AiTask.class));
    }

    @Test
    void failsTransactionOnOptimisticLockConflict() {
        AiTask task = task(AiTaskStatus.RUNNING, 2L);
        when(taskMapper.lockById("task-1")).thenReturn(task);
        when(taskMapper.updateAfterEvent(task)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.record(
                "task-1", null,
                event(LingXiEventType.NODE_PROGRESS, AiTaskStatus.RUNNING),
                "source-2", null, null, null));
        verify(broadcaster, never()).publish(any(LingXiEvent.class));
    }

    private AiTask task(String status, long sequence) {
        AiTask task = new AiTask();
        task.setId("task-1");
        task.setConversationId("conversation-1");
        task.setStatus(status);
        task.setProgress(0);
        task.setEventSequence(sequence);
        task.setVersion(1);
        return task;
    }

    private LingXiEvent event(String type, String status) {
        LingXiEvent event = new LingXiEvent();
        event.setEventType(type);
        event.setStatus(status);
        event.setPayload(Collections.emptyMap());
        return event;
    }
}
