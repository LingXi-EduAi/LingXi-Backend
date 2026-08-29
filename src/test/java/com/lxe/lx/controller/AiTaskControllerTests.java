package com.lxe.lx.controller;

import com.lxe.lx.domain.dto.AiApiResponse;
import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.domain.dto.AiTaskSnapshot;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiTaskControlService;
import com.lxe.lx.service.AiTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA-01 场景 1/2/3/6：AiTaskController 的流式/停止/订阅/查询委托与越权校验。
 * 断言 controller 把当前登录用户透传给 service/controlService，且缺失用户/空 query 被拒绝。
 */
class AiTaskControllerTests {
    private final AiTaskService taskService = mock(AiTaskService.class);
    private final AiTaskControlService controlService = mock(AiTaskControlService.class);
    private final AiTaskController controller = new AiTaskController(taskService, controlService);

    @Test
    void streamTaskDelegatesToServiceWithUserId() {
        HttpServletRequest request = request("user-1");
        SseEmitter emitter = new SseEmitter();
        when(taskService.streamTask(any(AiTaskRequest.class), eq("user-1"))).thenReturn(emitter);

        SseEmitter result = controller.streamTask(
                request, mock(HttpServletResponse.class), taskRequest("你好"));

        assertSame(emitter, result);
        verify(taskService).streamTask(any(AiTaskRequest.class), eq("user-1"));
    }

    @Test
    void streamTaskRejectsBlankQuery() {
        HttpServletRequest request = request("user-1");

        assertThrows(ResponseStatusException.class, () -> controller.streamTask(
                request, mock(HttpServletResponse.class), taskRequest("  ")));
        verify(taskService, never()).streamTask(any(AiTaskRequest.class), any());
    }

    @Test
    void streamTaskRejectsMissingUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThrows(ResponseStatusException.class, () -> controller.streamTask(
                request, mock(HttpServletResponse.class), taskRequest("你好")));
        verify(taskService, never()).streamTask(any(AiTaskRequest.class), any());
    }

    @Test
    void stopTaskDelegatesToControlServiceWithUserId() {
        HttpServletRequest request = request("user-1");
        AiTaskSnapshot snapshot = new AiTaskSnapshot();
        when(controlService.stop("task-1", "user-1")).thenReturn(snapshot);

        AiApiResponse<AiTaskSnapshot> response = controller.stopTask(request, "task-1");

        assertSame(snapshot, response.getData());
        verify(controlService).stop("task-1", "user-1");
    }

    @Test
    void subscribeTaskPassesLastEventIdToControlService() {
        HttpServletRequest request = request("user-1");
        SseEmitter emitter = new SseEmitter();
        when(controlService.subscribe("task-1", "user-1", "task-1:2")).thenReturn(emitter);

        SseEmitter result = controller.subscribeTask(
                request, mock(HttpServletResponse.class), "task-1", "task-1:2");

        assertSame(emitter, result);
        verify(controlService).subscribe("task-1", "user-1", "task-1:2");
    }

    @Test
    void getTaskDelegatesToControlServiceWithUserId() {
        HttpServletRequest request = request("user-1");
        AiTaskSnapshot snapshot = new AiTaskSnapshot();
        when(controlService.getSnapshot("task-1", "user-1")).thenReturn(snapshot);

        AiApiResponse<AiTaskSnapshot> response = controller.getTask(request, "task-1");

        assertSame(snapshot, response.getData());
        verify(controlService).getSnapshot("task-1", "user-1");
    }

    private HttpServletRequest request(String userId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        TokenEntity token = new TokenEntity();
        token.setId(userId);
        when(request.getAttribute(ORG_ID_KEY)).thenReturn(token);
        return request;
    }

    private AiTaskRequest taskRequest(String query) {
        AiTaskRequest request = new AiTaskRequest();
        request.setQuery(query);
        return request;
    }
}