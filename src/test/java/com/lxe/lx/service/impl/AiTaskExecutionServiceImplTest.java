package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.domain.dto.LingXiEventType;
import com.lxe.lx.gateway.DifyEventAdapter;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.gateway.DifyStream;
import com.lxe.lx.gateway.DifyStreamListener;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiModelCallLog;
import com.lxe.lx.pojo.AiSubtask;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiEventService;
import com.lxe.lx.service.AiModelCallLogService;
import com.lxe.lx.service.AiRuntimeRegistry;
import com.lxe.lx.service.AiTaskResultPersistenceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskExecutionServiceImplTest {
    private final AiTaskMapper taskMapper = mock(AiTaskMapper.class);
    private final AiSubtaskMapper subtaskMapper = mock(AiSubtaskMapper.class);
    private final AiEventService eventService = mock(AiEventService.class);
    private final DifyGateway difyGateway = mock(DifyGateway.class);
    private final AiRuntimeRegistry runtimeRegistry = mock(AiRuntimeRegistry.class);
    private final AiTaskResultPersistenceService resultPersistence =
            mock(AiTaskResultPersistenceService.class);
    private final AiModelCallLogService modelCallLogService = mock(AiModelCallLogService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiTaskExecutionServiceImpl service = new AiTaskExecutionServiceImpl(
            taskMapper, subtaskMapper, eventService, difyGateway,
            mock(com.lxe.lx.gateway.AiAgentRouter.class),
            new DifyEventAdapter(objectMapper), runtimeRegistry, objectMapper,
            resultPersistence, modelCallLogService);

    @Test
    void recordsOneModelCallLogPerWorkflowNodeWithRealNames() throws Exception {
        AiTask task = task("WORKFLOW");
        when(taskMapper.findById("task-1")).thenReturn(task);
        when(subtaskMapper.findByTaskId("task-1"))
                .thenReturn(Collections.singletonList(subtask()));

        DifyStreamListener[] captured = new DifyStreamListener[1];
        when(difyGateway.streamWorkflow(anyMap(), eq("user-1"), any(DifyStreamListener.class)))
                .thenAnswer(invocation -> {
                    captured[0] = invocation.getArgument(2);
                    return mock(DifyStream.class);
                });

        service.execute("task-1");

        DifyStreamListener listener = captured[0];
        listener.onEvent(nodeEvent("node_started", "node-1", "知识检索", "gpt-4o"));
        listener.onEvent(nodeEvent("node_started", "node-2", "答案生成", "gpt-4o"));
        listener.onEvent(nodeEvent("node_finished", "node-2", "答案生成", "gpt-4o"));
        listener.onEvent(workflowFinished());

        ArgumentCaptor<AiModelCallLog> captor = ArgumentCaptor.forClass(AiModelCallLog.class);
        verify(modelCallLogService, org.mockito.Mockito.times(2))
                .recordAsync(captor.capture());
        List<AiModelCallLog> logs = captor.getAllValues();
        assertEquals(2, logs.size());
        assertEquals("知识检索", logs.get(0).getNodeName());
        assertEquals("答案生成", logs.get(1).getNodeName());
        assertEquals("gpt-4o", logs.get(0).getModel());
        assertEquals("gpt-4o", logs.get(1).getModel());
    }

    @Test
    void fallsBackToTaskTypeWhenNodeAndModelAbsent() throws Exception {
        AiTask task = task("WORKFLOW");
        when(taskMapper.findById("task-1")).thenReturn(task);
        when(subtaskMapper.findByTaskId("task-1"))
                .thenReturn(Collections.singletonList(subtask()));

        DifyStreamListener[] captured = new DifyStreamListener[1];
        when(difyGateway.streamWorkflow(anyMap(), eq("user-1"), any(DifyStreamListener.class)))
                .thenAnswer(invocation -> {
                    captured[0] = invocation.getArgument(2);
                    return mock(DifyStream.class);
                });

        service.execute("task-1");

        DifyStreamListener listener = captured[0];
        listener.onEvent(nodeEvent("node_started", "", "", ""));
        listener.onEvent(workflowFinished());

        ArgumentCaptor<AiModelCallLog> captor = ArgumentCaptor.forClass(AiModelCallLog.class);
        verify(modelCallLogService).recordAsync(captor.capture());
        assertEquals("WORKFLOW", captor.getValue().getNodeName());
        assertEquals("WORKFLOW", captor.getValue().getModel());
    }

    @Test
    void onErrorRecordsErrorEventAndErrorModelLog() throws Exception {
        AiTask task = task("WORKFLOW");
        when(taskMapper.findById("task-1")).thenReturn(task);
        when(subtaskMapper.findByTaskId("task-1"))
                .thenReturn(Collections.singletonList(subtask()));

        DifyStreamListener[] captured = new DifyStreamListener[1];
        when(difyGateway.streamWorkflow(anyMap(), eq("user-1"), any(DifyStreamListener.class)))
                .thenAnswer(invocation -> {
                    captured[0] = invocation.getArgument(2);
                    return mock(DifyStream.class);
                });

        service.execute("task-1");

        captured[0].onError(new DifyGatewayException("Dify 连接超时", null, true, null));

        ArgumentCaptor<LingXiEvent> eventCaptor = ArgumentCaptor.forClass(LingXiEvent.class);
        verify(eventService).record(eq("task-1"), eq("subtask-1"), eventCaptor.capture(),
                any(), eq(null), eq("DIFY_STREAM_ERROR"), eq("Dify 连接超时"));
        assertEquals(LingXiEventType.TASK_ERROR, eventCaptor.getValue().getEventType());
        assertEquals("FAILED", eventCaptor.getValue().getStatus());
        assertEquals(true, eventCaptor.getValue().getPayload().get("retryable"));

        ArgumentCaptor<AiModelCallLog> logCaptor = ArgumentCaptor.forClass(AiModelCallLog.class);
        verify(modelCallLogService).recordAsync(logCaptor.capture());
        assertEquals("DIFY_STREAM_ERROR", logCaptor.getValue().getErrorCode());
        verify(runtimeRegistry).remove("task-1");
    }

    @Test
    void streamSetupTimeoutFailsTaskGracefully() throws Exception {
        AiTask task = task("WORKFLOW");
        when(taskMapper.findById("task-1")).thenReturn(task);
        when(subtaskMapper.findByTaskId("task-1"))
                .thenReturn(Collections.singletonList(subtask()));
        when(difyGateway.streamWorkflow(anyMap(), eq("user-1"), any(DifyStreamListener.class)))
                .thenThrow(new DifyGatewayException(
                        "无法连接 Dify 服务：Workflow 流式调用", null, true, null));

        service.execute("task-1");

        ArgumentCaptor<LingXiEvent> eventCaptor = ArgumentCaptor.forClass(LingXiEvent.class);
        verify(eventService).record(eq("task-1"), eq("subtask-1"), eventCaptor.capture(),
                any(), eq(null), eq("DIFY_STREAM_ERROR"),
                eq("无法连接 Dify 服务：Workflow 流式调用"));
        assertEquals(LingXiEventType.TASK_ERROR, eventCaptor.getValue().getEventType());
        assertEquals("FAILED", eventCaptor.getValue().getStatus());
        assertEquals(true, eventCaptor.getValue().getPayload().get("retryable"));
    }

    @Test
    void difyErrorEventPersistsErrorCodeAndMessage() throws Exception {
        AiTask task = task("WORKFLOW");
        when(taskMapper.findById("task-1")).thenReturn(task);
        when(subtaskMapper.findByTaskId("task-1"))
                .thenReturn(Collections.singletonList(subtask()));

        DifyStreamListener[] captured = new DifyStreamListener[1];
        when(difyGateway.streamWorkflow(anyMap(), eq("user-1"), any(DifyStreamListener.class)))
                .thenAnswer(invocation -> {
                    captured[0] = invocation.getArgument(2);
                    return mock(DifyStream.class);
                });

        service.execute("task-1");

        captured[0].onEvent(errorEvent());

        ArgumentCaptor<LingXiEvent> eventCaptor = ArgumentCaptor.forClass(LingXiEvent.class);
        verify(resultPersistence).recordTerminalEvent(
                eq("task-1"), eq("subtask-1"), eventCaptor.capture(),
                eq(null), eq(null), eq("upstream_error"), eq("服务暂时不可用"),
                eq(null), eq(null), any());
        assertEquals(LingXiEventType.TASK_ERROR, eventCaptor.getValue().getEventType());
        assertEquals("FAILED", eventCaptor.getValue().getStatus());

        ArgumentCaptor<AiModelCallLog> logCaptor = ArgumentCaptor.forClass(AiModelCallLog.class);
        verify(modelCallLogService).recordAsync(logCaptor.capture());
        assertEquals("upstream_error", logCaptor.getValue().getErrorCode());
        verify(runtimeRegistry).remove("task-1");
    }

    private JsonNode errorEvent() throws Exception {
        String json = "{\"event\":\"error\",\"status\":503,\"code\":\"upstream_error\","
                + "\"message\":\"服务暂时不可用\"}";
        return objectMapper.readTree(json);
    }

    private JsonNode nodeEvent(String event, String nodeId, String title, String model) throws Exception {
        String json = "{\"event\":\"" + event + "\",\"task_id\":\"dify-1\","
                + "\"data\":{\"node_id\":\"" + nodeId + "\",\"title\":\"" + title + "\"},"
                + "\"metadata\":{\"usage\":{\"model\":\"" + model + "\",\"total_tokens\":100}}}";
        return objectMapper.readTree(json);
    }

    private JsonNode workflowFinished() throws Exception {
        String json = "{\"event\":\"workflow_finished\",\"task_id\":\"dify-1\","
                + "\"data\":{\"status\":\"succeeded\",\"outputs\":{\"answer\":\"done\"}}}";
        return objectMapper.readTree(json);
    }

    private AiTask task(String taskType) throws Exception {
        AiTask task = new AiTask();
        task.setId("task-1");
        task.setUserId("user-1");
        task.setConversationId("conversation-1");
        task.setTaskType(taskType);
        task.setStatus("CREATED");
        task.setRequestJson("{\"taskType\":\"WORKFLOW\",\"inputs\":{\"topic\":\"x\"}}");
        return task;
    }

    private AiSubtask subtask() {
        AiSubtask subtask = new AiSubtask();
        subtask.setId("subtask-1");
        subtask.setTaskId("task-1");
        subtask.setAgentType("WORKFLOW");
        subtask.setGoal("goal");
        subtask.setInputsJson("{}");
        subtask.setDependencyJson("[]");
        subtask.setStatus("CREATED");
        subtask.setExecutionNo(1);
        subtask.setRetryCount(0);
        subtask.setVersion(1);
        return subtask;
    }
}
