package com.lxe.lx.service.impl;

import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.gateway.DifyChatApplication;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiSubtask;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiTaskRecoveryService 单元测试（纯 JUnit 5 + Mockito，无 Spring 上下文）。
 *
 * <p>{@code recoverInterruptedTasks()} 是普通方法（{@code @EventListener(ApplicationReadyEvent)}
 * 只是启动时的触发方式），测试直接调用方法本身。</p>
 */
class AiTaskRecoveryServiceTest {

    private AiTaskMapper taskMapper;
    private AiSubtaskMapper subtaskMapper;
    private AiEventService eventService;
    private DifyGateway difyGateway;
    private AiTaskRecoveryService service;

    @BeforeEach
    void setUp() {
        taskMapper = mock(AiTaskMapper.class);
        subtaskMapper = mock(AiSubtaskMapper.class);
        eventService = mock(AiEventService.class);
        difyGateway = mock(DifyGateway.class);
        service = new AiTaskRecoveryService(taskMapper, subtaskMapper, eventService, difyGateway, true);
    }

    private AiTaskRecoveryService disabledService() {
        return new AiTaskRecoveryService(taskMapper, subtaskMapper, eventService, difyGateway, false);
    }

    private AiTask task(String id, String taskType, String difyTaskId) {
        AiTask task = new AiTask();
        task.setId(id);
        task.setUserId("user-1");
        task.setTaskType(taskType);
        task.setStatus("RUNNING");
        task.setDifyTaskId(difyTaskId);
        return task;
    }

    private AiSubtask subtask(String id) {
        AiSubtask subtask = new AiSubtask();
        subtask.setId(id);
        subtask.setTaskId("task-1");
        return subtask;
    }

    @Test
    void disabledRecoveryDoesNothing() {
        disabledService().recoverInterruptedTasks();

        verify(taskMapper, never()).findRunningTasks();
        verify(eventService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void noRunningTasksDoesNothing() {
        when(taskMapper.findRunningTasks()).thenReturn(Collections.emptyList());

        service.recoverInterruptedTasks();

        verify(eventService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(difyGateway, never()).stopChatMessage(any(), any(), any());
        verify(difyGateway, never()).stopWorkflow(any(), any());
    }

    @Test
    void recoversRunningTaskToFailedWithInterruptedEvent() {
        AiTask task = task("task-1", "CHATFLOW", "dify-1");
        when(taskMapper.findRunningTasks()).thenReturn(Collections.singletonList(task));
        when(subtaskMapper.findByTaskId("task-1")).thenReturn(Collections.singletonList(subtask("subtask-1")));

        service.recoverInterruptedTasks();

        ArgumentCaptor<LingXiEvent> eventCaptor = ArgumentCaptor.forClass(LingXiEvent.class);
        verify(eventService).record(eq("task-1"), eq("subtask-1"), eventCaptor.capture(),
                eq("lingxi:recovery:task-1"), eq(null), eq("EXECUTION_INTERRUPTED"),
                eq("后端重启，任务中断（可重新提问）"));
        LingXiEvent event = eventCaptor.getValue();
        assertEquals(LingXiEventType.EXECUTION_INTERRUPTED, event.getEventType());
        assertEquals("FAILED", event.getStatus());
        assertEquals("EXECUTION_INTERRUPTED", event.getPayload().get("code"));
        assertEquals("后端重启，任务中断（可重新提问）", event.getPayload().get("message"));
        assertEquals(true, event.getPayload().get("retryable"));
        verify(difyGateway).stopChatMessage(DifyChatApplication.CHATFLOW, "dify-1", "user-1");
    }

    @Test
    void recoversWorkflowTaskViaStopWorkflow() {
        AiTask task = task("task-1", "WORKFLOW", "dify-1");
        when(taskMapper.findRunningTasks()).thenReturn(Collections.singletonList(task));
        when(subtaskMapper.findByTaskId("task-1")).thenReturn(Collections.emptyList());

        service.recoverInterruptedTasks();

        verify(eventService).record(eq("task-1"), eq(null), any(), eq("lingxi:recovery:task-1"),
                eq(null), eq("EXECUTION_INTERRUPTED"), eq("后端重启，任务中断（可重新提问）"));
        verify(difyGateway).stopWorkflow("dify-1", "user-1");
    }

    @Test
    void skipsDifyStopWhenNoDifyTaskId() {
        AiTask task = task("task-1", "CHATFLOW", null);
        when(taskMapper.findRunningTasks()).thenReturn(Collections.singletonList(task));
        when(subtaskMapper.findByTaskId("task-1")).thenReturn(Collections.emptyList());

        service.recoverInterruptedTasks();

        verify(eventService).record(eq("task-1"), eq(null), any(), eq("lingxi:recovery:task-1"),
                eq(null), eq("EXECUTION_INTERRUPTED"), eq("后端重启，任务中断（可重新提问）"));
        verify(difyGateway, never()).stopChatMessage(any(), any(), any());
        verify(difyGateway, never()).stopWorkflow(any(), any());
    }

    @Test
    void difyStopFailureDoesNotBlockOtherTasks() {
        AiTask task1 = task("task-1", "CHATFLOW", "dify-1");
        AiTask task2 = task("task-2", "CHATFLOW", "dify-2");
        when(taskMapper.findRunningTasks()).thenReturn(Arrays.asList(task1, task2));
        when(subtaskMapper.findByTaskId(any())).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("network down"))
                .when(difyGateway).stopChatMessage(DifyChatApplication.CHATFLOW, "dify-1", "user-1");

        service.recoverInterruptedTasks();

        verify(eventService).record(eq("task-1"), eq(null), any(), eq("lingxi:recovery:task-1"),
                eq(null), eq("EXECUTION_INTERRUPTED"), eq("后端重启，任务中断（可重新提问）"));
        verify(eventService).record(eq("task-2"), eq(null), any(), eq("lingxi:recovery:task-2"),
                eq(null), eq("EXECUTION_INTERRUPTED"), eq("后端重启，任务中断（可重新提问）"));
    }

    @Test
    void eventRecordFailureDoesNotBlockOtherTasks() {
        AiTask task1 = task("task-1", "CHATFLOW", null);
        AiTask task2 = task("task-2", "CHATFLOW", null);
        when(taskMapper.findRunningTasks()).thenReturn(Arrays.asList(task1, task2));
        when(subtaskMapper.findByTaskId(any())).thenReturn(Collections.emptyList());
        when(eventService.record(eq("task-1"), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("db down"));

        service.recoverInterruptedTasks();

        verify(eventService).record(eq("task-2"), eq(null), any(), eq("lingxi:recovery:task-2"),
                eq(null), eq("EXECUTION_INTERRUPTED"), eq("后端重启，任务中断（可重新提问）"));
    }
}