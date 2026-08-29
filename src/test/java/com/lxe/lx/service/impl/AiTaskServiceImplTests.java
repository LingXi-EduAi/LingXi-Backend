package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.gateway.AiAgentRouter;
import com.lxe.lx.gateway.DifyEventAdapter;
import com.lxe.lx.gateway.DifyGateway;
import com.lxe.lx.gateway.DifyGatewayException;
import com.lxe.lx.gateway.DifyStream;
import com.lxe.lx.gateway.DifyStreamListener;
import com.lxe.lx.mapper.AiSubtaskMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.service.AiConversationService;
import com.lxe.lx.service.AiMessageService;
import com.lxe.lx.service.AiTaskExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA-01 场景 1/5：AiTaskServiceImpl.streamTask 的 SSE 流式主链路。
 * Mock DifyGateway，不依赖真实 Dify 服务。断言到 service 层：
 * 返回 SseEmitter、事件转发调用链、正常结束/异常处理后的 emitter 状态。
 */
class AiTaskServiceImplTests {
    private final DifyGateway difyGateway = mock(DifyGateway.class);
    private final AiAgentRouter agentRouter = mock(AiAgentRouter.class);
    private final DifyEventAdapter eventAdapter = new DifyEventAdapter(new ObjectMapper());
    private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
    private final AiTaskServiceImpl service = new AiTaskServiceImpl(
            difyGateway, agentRouter, eventAdapter, taskScheduler,
            mock(AiTaskMapper.class), mock(AiSubtaskMapper.class),
            mock(AiTaskExecutionService.class), mock(TaskExecutor.class), new ObjectMapper(),
            mock(AiMessageService.class), mock(AiConversationService.class),
            600000, 15000);

    @Test
    void streamTaskForwardsDifyEventsAndCompletesOnStreamEnd() throws Exception {
        DifyStreamListener[] captured = new DifyStreamListener[1];
        when(difyGateway.streamChatMessage(any(), any(), eq("user-1"), any(DifyStreamListener.class)))
                .thenAnswer(invocation -> {
                    captured[0] = invocation.getArgument(3);
                    return mock(DifyStream.class);
                });

        SseEmitter emitter = service.streamTask(request("你好"), "user-1");

        // 流未结束前 emitter 保持打开（无 handler 时事件被缓冲，不抛异常）
        assertDoesNotThrow(() -> emitter.send("still-open"));

        captured[0].onEvent(messageEvent());
        captured[0].onComplete();

        // onComplete 触发 session.complete()，emitter 已关闭，拒绝后续发送
        assertThrows(IllegalStateException.class, () -> emitter.send("after-complete"));
        verify(difyGateway).streamChatMessage(any(), any(), eq("user-1"), any(DifyStreamListener.class));
    }

    @Test
    void streamTaskSendsErrorEventAndCompletesOnDifyError() throws Exception {
        DifyStreamListener[] captured = new DifyStreamListener[1];
        when(difyGateway.streamChatMessage(any(), any(), eq("user-1"), any(DifyStreamListener.class)))
                .thenAnswer(invocation -> {
                    captured[0] = invocation.getArgument(3);
                    return mock(DifyStream.class);
                });

        SseEmitter emitter = service.streamTask(request("你好"), "user-1");

        captured[0].onError(new DifyGatewayException("Dify 连接超时", null, true, null));

        // onError 发送 streamError 事件后 complete，emitter 已关闭
        assertThrows(IllegalStateException.class, () -> emitter.send("after-error"));
    }

    @Test
    void streamTaskCompletesWithErrorEventWhenSetupFails() throws Exception {
        when(difyGateway.streamChatMessage(any(), any(), eq("user-1"), any(DifyStreamListener.class)))
                .thenThrow(new DifyGatewayException("无法连接 Dify 服务：Chatflow 流式调用", null, true, null));

        SseEmitter emitter = service.streamTask(request("你好"), "user-1");

        // setup 失败路径：发送 streamError 事件后 complete，不向调用方抛异常
        assertThrows(IllegalStateException.class, () -> emitter.send("after-setup-error"));
    }

    private AiTaskRequest request(String query) {
        AiTaskRequest request = new AiTaskRequest();
        request.setQuery(query);
        return request;
    }

    private com.fasterxml.jackson.databind.JsonNode messageEvent() throws Exception {
        return new ObjectMapper().readTree(
                "{\"event\":\"message\",\"task_id\":\"dify-1\",\"message_id\":\"message-1\","
                        + "\"conversation_id\":\"conversation-1\",\"answer\":\"2\"}"
        );
    }
}