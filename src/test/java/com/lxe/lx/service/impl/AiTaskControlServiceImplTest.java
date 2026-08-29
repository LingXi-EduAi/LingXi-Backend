package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.AiTaskStatus;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiEvent;
import com.lxe.lx.pojo.AiSubtask;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiEventService;
import com.lxe.lx.service.AiRuntimeRegistry;
import com.lxe.lx.service.AiTaskApiException;
import com.lxe.lx.service.AiTaskExecutionService;
import com.lxe.lx.service.TaskEventBroadcaster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskControlServiceImplTest {
    private AiTaskMapper taskMapper;
    private AiSubtaskMapper subtaskMapper;
    private AiEventMapper eventMapper;
    private AiEventService eventService;
    private TaskEventBroadcaster broadcaster;
    private AiRuntimeRegistry runtimeRegistry;
    private DifyGateway difyGateway;
    private AiTaskExecutionService executionService;
    private TaskExecutor executor;
    private AiTaskControlServiceImpl service;

    @BeforeEach
    void setUp() {
        taskMapper = mock(AiTaskMapper.class);
        subtaskMapper = mock(AiSubtaskMapper.class);
        eventMapper = mock(AiEventMapper.class);
        eventService = mock(AiEventService.class);
        broadcaster = mock(TaskEventBroadcaster.class);
        runtimeRegistry = mock(AiRuntimeRegistry.class);
        difyGateway = mock(DifyGateway.class);
        executionService = mock(AiTaskExecutionService.class);
        executor = mock(TaskExecutor.class);
        service = new AiTaskControlServiceImpl(
                taskMapper,
                subtaskMapper,
                eventMapper,
                eventService,
                broadcaster,
                runtimeRegistry,
                difyGateway,
                executionService,
                executor,
                mock(TaskScheduler.class),
                new ObjectMapper(),
                3,
                600000,
                15000);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void hidesTaskOwnedByAnotherUser() {
        when(taskMapper.findByIdAndUser("task-1", "user-2")).thenReturn(null);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.getSnapshot("task-1", "user-2"));

        assertEquals(404, exception.getHttpStatus());
    }

    @Test
    void rejectsMismatchedLastEventTaskId() {
        AiTask task = task(AiTaskStatus.RUNNING);
        task.setEventSequence(4L);
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(task);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.subscribe("task-1", "user-1", "other-task:2"));

        assertEquals(400, exception.getHttpStatus());
    }

    @Test
    void subscribeReplaysEventsAfterLastEventId() {
        AiTask running = task(AiTaskStatus.RUNNING);
        running.setEventSequence(4L);
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(running);
        AiEvent event3 = event(3L);
        AiEvent event4 = event(4L);
        when(eventMapper.findAfterSequence("task-1", 2)).thenReturn(Arrays.asList(event3, event4));
        when(eventService.toContract(event3, "conversation-1")).thenReturn(contract(3L));
        when(eventService.toContract(event4, "conversation-1")).thenReturn(contract(4L));
        when(broadcaster.subscribe(any(), any()))
                .thenReturn(mock(TaskEventBroadcaster.Subscription.class));

        SseEmitter emitter = service.subscribe("task-1", "user-1", "task-1:2");

        assertNotNull(emitter);
        verify(eventMapper).findAfterSequence("task-1", 2);
        verify(eventService).toContract(event3, "conversation-1");
        verify(eventService).toContract(event4, "conversation-1");
        verify(broadcaster).subscribe(eq("task-1"), any());
    }

    @Test
    void subscribeRejectsSequenceBeyondTaskRange() {
        AiTask running = task(AiTaskStatus.RUNNING);
        running.setEventSequence(4L);
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(running);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.subscribe("task-1", "user-1", "task-1:5"));

        assertEquals(400, exception.getHttpStatus());
        verify(eventMapper, never()).findAfterSequence(any(), anyLong());
    }

    @Test
    void subscribeCompletesWhenTerminalAndUpToDate() {
        AiTask succeeded = task(AiTaskStatus.SUCCEEDED);
        succeeded.setEventSequence(4L);
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(succeeded);
        when(eventMapper.findAfterSequence("task-1", 4)).thenReturn(Collections.emptyList());
        when(broadcaster.subscribe(any(), any()))
                .thenReturn(mock(TaskEventBroadcaster.Subscription.class));

        SseEmitter emitter = service.subscribe("task-1", "user-1", "task-1:4");

        assertNotNull(emitter);
        // 终态且已消费到最新事件 → session.complete()，emitter 拒绝后续发送
        assertThrows(IllegalStateException.class, () -> emitter.send("after-complete"));
    }

    @Test
    void subscribeHidesTaskOwnedByAnotherUser() {
        when(taskMapper.findByIdAndUser("task-1", "user-2")).thenReturn(null);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.subscribe("task-1", "user-2", null));

        assertEquals(404, exception.getHttpStatus());
        verify(eventMapper, never()).findAfterSequence(any(), anyLong());
    }

    @Test
    void stopHidesTaskOwnedByAnotherUser() {
        when(taskMapper.lockById("task-1")).thenReturn(task(AiTaskStatus.RUNNING));

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.stop("task-1", "user-2"));

        assertEquals(404, exception.getHttpStatus());
        verify(eventService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void stopsDatabaseStateBeforeCancelingRuntime() {
        AiTask running = task(AiTaskStatus.RUNNING);
        running.setDifyTaskId("dify-1");
        AiTask stopped = task(AiTaskStatus.STOPPED);
        AiSubtask subtask = subtask(AiTaskStatus.RUNNING, 0);
        when(taskMapper.lockById("task-1")).thenReturn(running);
        when(subtaskMapper.findByTaskId("task-1")).thenReturn(Collections.singletonList(subtask));
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(stopped);

        service.stop("task-1", "user-1");

        verify(eventService).record(eq("task-1"), eq("subtask-1"), any(),
                eq("lingxi:user_stopped"), eq(null), eq(null), eq(null));
        verify(runtimeRegistry, never()).cancel("task-1");
        commitCallbacks();
        verify(runtimeRegistry).cancel("task-1");
        verify(difyGateway).stopChatMessage(any(), eq("dify-1"), eq("user-1"));
    }

    @Test
    void repeatedStopIsIdempotent() {
        AiTask stopped = task(AiTaskStatus.STOPPED);
        when(taskMapper.lockById("task-1")).thenReturn(stopped);
        when(subtaskMapper.findByTaskId("task-1")).thenReturn(Collections.emptyList());

        service.stop("task-1", "user-1");

        verify(eventService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(runtimeRegistry, never()).cancel(any());
    }

    @Test
    void rejectsRetryAtConfiguredLimit() {
        AiTask failed = task(AiTaskStatus.FAILED);
        AiSubtask subtask = subtask(AiTaskStatus.FAILED, 3);
        when(taskMapper.lockById("task-1")).thenReturn(failed);
        when(subtaskMapper.lockByIdAndTask("subtask-1", "task-1")).thenReturn(subtask);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.retry("task-1", "subtask-1", "user-1"));

        assertEquals(409, exception.getHttpStatus());
        verify(subtaskMapper, never()).prepareRetry(any());
    }

    @Test
    void retriesSameLogicalSubtaskAfterCommit() {
        AiTask failed = task(AiTaskStatus.FAILED);
        AiTask running = task(AiTaskStatus.RUNNING);
        AiSubtask subtask = subtask(AiTaskStatus.FAILED, 0);
        when(taskMapper.lockById("task-1")).thenReturn(failed);
        when(subtaskMapper.lockByIdAndTask("subtask-1", "task-1")).thenReturn(subtask);
        when(subtaskMapper.findByTaskId("task-1")).thenReturn(Collections.singletonList(subtask));
        when(subtaskMapper.prepareRetry(subtask)).thenReturn(1);
        when(taskMapper.restartForRetry(failed)).thenReturn(1);
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(running);

        service.retry("task-1", "subtask-1", "user-1");

        assertEquals(2, subtask.getExecutionNo());
        assertEquals(1, subtask.getRetryCount());
        verify(executor, never()).execute(any());
        commitCallbacks();
        verify(executor).execute(any());
    }

    @Test
    void rejectsRetryWhenDependencyHasNotSucceeded() {
        AiTask failed = task(AiTaskStatus.FAILED);
        AiSubtask target = subtask(AiTaskStatus.FAILED, 0);
        target.setDependencyJson("[\"dependency-1\"]");
        AiSubtask dependency = subtask(AiTaskStatus.RUNNING, 0);
        dependency.setId("dependency-1");
        when(taskMapper.lockById("task-1")).thenReturn(failed);
        when(subtaskMapper.lockByIdAndTask("subtask-1", "task-1")).thenReturn(target);
        when(subtaskMapper.findByTaskId("task-1"))
                .thenReturn(java.util.Arrays.asList(target, dependency));

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.retry("task-1", "subtask-1", "user-1"));

        assertEquals(409, exception.getHttpStatus());
        verify(subtaskMapper, never()).prepareRetry(any());
    }

    @Test
    void rejectsConcurrentDuplicateRetry() {
        AiTask failed = task(AiTaskStatus.FAILED);
        AiSubtask subtask = subtask(AiTaskStatus.FAILED, 0);
        when(taskMapper.lockById("task-1")).thenReturn(failed);
        when(subtaskMapper.lockByIdAndTask("subtask-1", "task-1")).thenReturn(subtask);
        when(subtaskMapper.findByTaskId("task-1")).thenReturn(Collections.singletonList(subtask));
        when(subtaskMapper.prepareRetry(subtask)).thenReturn(0);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.retry("task-1", "subtask-1", "user-1"));

        assertEquals(409, exception.getHttpStatus());
        verify(taskMapper, never()).restartForRetry(any());
    }

    private void commitCallbacks() {
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
    }

    private AiTask task(String status) {
        AiTask task = new AiTask();
        task.setId("task-1");
        task.setUserId("user-1");
        task.setConversationId("conversation-1");
        task.setTaskType("CHATFLOW");
        task.setStatus(status);
        task.setProgress(10);
        task.setVersion(1);
        task.setEventSequence(2L);
        return task;
    }

    private AiSubtask subtask(String status, int retries) {
        AiSubtask subtask = new AiSubtask();
        subtask.setId("subtask-1");
        subtask.setTaskId("task-1");
        subtask.setAgentType("CHATFLOW");
        subtask.setGoal("retry goal");
        subtask.setInputsJson("{}");
        subtask.setDependencyJson("[]");
        subtask.setStatus(status);
        subtask.setExecutionNo(1);
        subtask.setRetryCount(retries);
        subtask.setVersion(1);
        return subtask;
    }

    private AiEvent event(long sequence) {
        AiEvent event = new AiEvent();
        event.setTaskId("task-1");
        event.setSequence(sequence);
        event.setEventType("node_progress");
        event.setStatus("RUNNING");
        return event;
    }

    private LingXiEvent contract(long sequence) {
        LingXiEvent event = new LingXiEvent();
        event.setEventId("task-1:" + sequence);
        event.setSequence(sequence);
        event.setEventType("node_progress");
        event.setStatus("RUNNING");
        event.setPayload(Collections.emptyMap());
        return event;
    }
}

