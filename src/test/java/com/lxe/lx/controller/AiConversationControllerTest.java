package com.lxe.lx.controller;

import com.lxe.lx.domain.dto.AiConversationContinueRequest;
import com.lxe.lx.domain.dto.AiTaskRequest;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.AiConversationService;
import com.lxe.lx.service.AiTaskApiException;
import com.lxe.lx.service.AiTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConversationControllerTest {
    private final AiConversationService conversationService = mock(AiConversationService.class);
    private final AiTaskService taskService = mock(AiTaskService.class);
    private final AiConversationController controller =
            new AiConversationController(conversationService, taskService);

    @Test
    void continueConversationChecksOwnershipThenStreamsWithConversationId() {
        HttpServletRequest request = request("user-1");
        SseEmitter emitter = new SseEmitter();
        when(taskService.streamTask(any(AiTaskRequest.class), eq("user-1"))).thenReturn(emitter);

        AiConversationContinueRequest body = new AiConversationContinueRequest();
        body.setQuery("继续分析");
        SseEmitter result = controller.continueConversation(request, "conversation-1", body);

        verify(conversationService).requireActiveOwned("conversation-1", "user-1");
        verify(taskService).streamTask(any(AiTaskRequest.class), eq("user-1"));
        assertSame(emitter, result);
    }

    @Test
    void continueConversationRejectsBlankQuery() {
        HttpServletRequest request = request("user-1");
        AiConversationContinueRequest body = new AiConversationContinueRequest();
        body.setQuery("  ");

        assertThrows(AiTaskApiException.class,
                () -> controller.continueConversation(request, "conversation-1", body));
        verify(taskService, never()).streamTask(any(AiTaskRequest.class), any());
    }

    private HttpServletRequest request(String userId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        TokenEntity token = new TokenEntity();
        token.setId(userId);
        when(request.getAttribute(ORG_ID_KEY)).thenReturn(token);
        return request;
    }
}
