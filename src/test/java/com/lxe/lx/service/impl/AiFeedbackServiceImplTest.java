package com.lxe.lx.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxe.lx.domain.dto.AiFeedbackView;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiEvidenceMapper;
import com.lxe.lx.mapper.AiFeedbackMapper;
import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiEvent;
import com.lxe.lx.pojo.AiEvidence;
import com.lxe.lx.pojo.AiFeedback;
import com.lxe.lx.pojo.AiMessage;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiTaskApiException;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiFeedbackServiceImplTest {

    private static final String VALIDATION_PAYLOAD = "{"
            + "\"conclusion\":\"基础掌握，细节需加强\","
            + "\"checks\":["
            + "{\"name\":\"求根公式\",\"passed\":true},"
            + "{\"name\":\"判别式符号\",\"status\":\"FAIL\",\"detail\":\"符号判断错误\"},"
            + "{\"title\":\"因式分解\",\"passed\":false}],"
            + "\"suggestions\":[\"复习判别式\",\"完成 3 道判别式练习题\"]}";

    private final AiFeedbackMapper feedbackMapper = mock(AiFeedbackMapper.class);
    private final AiTaskMapper taskMapper = mock(AiTaskMapper.class);
    private final AiEventMapper eventMapper = mock(AiEventMapper.class);
    private final AiMessageMapper messageMapper = mock(AiMessageMapper.class);
    private final AiEvidenceMapper evidenceMapper = mock(AiEvidenceMapper.class);

    private final AiFeedbackServiceImpl service = new AiFeedbackServiceImpl(
            feedbackMapper, taskMapper, eventMapper, messageMapper, evidenceMapper, new ObjectMapper());

    @Test
    void generatesFeedbackFromValidationEvent() {
        AiTask task = task();
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(task);
        when(feedbackMapper.findByTaskId("task-1")).thenReturn(null);
        when(eventMapper.findLatestByTaskAndType("task-1", "validation_finished"))
                .thenReturn(event(VALIDATION_PAYLOAD));
        when(messageMapper.findByTaskAndRole("task-1", "assistant"))
                .thenReturn(message("answer-1"));
        AiEvidence evidence = new AiEvidence();
        evidence.setTitle("人教版数学必修一");
        when(evidenceMapper.findByMessageId("answer-1")).thenReturn(Collections.singletonList(evidence));

        AiFeedbackView view = service.getOrGenerate("task-1", "user-1");

        assertEquals("VALIDATION_FINISHED", view.getSource());
        assertEquals("基础掌握，细节需加强", view.getSummary());
        assertEquals(2, view.getWeakPoints().size());
        assertEquals("判别式符号：符号判断错误", view.getWeakPoints().get(0));
        assertEquals("因式分解", view.getWeakPoints().get(1));
        assertEquals(2, view.getRecommendations().size());
        assertEquals("复习判别式", view.getRecommendations().get(0));
        verify(feedbackMapper).insert(any(AiFeedback.class));
    }

    @Test
    void fallsBackToLocalRulesWhenNoReflectionEvent() {
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(task());
        when(feedbackMapper.findByTaskId("task-1")).thenReturn(null);
        when(eventMapper.findLatestByTaskAndType("task-1", "validation_finished")).thenReturn(null);
        when(messageMapper.findByTaskAndRole("task-1", "assistant")).thenReturn(message("answer-1"));
        when(evidenceMapper.findByMessageId("answer-1")).thenReturn(Collections.emptyList());

        AiFeedbackView view = service.getOrGenerate("task-1", "user-1");

        assertEquals("RULE_FALLBACK", view.getSource());
        assertFalse(view.getWeakPoints().isEmpty());
        assertFalse(view.getRecommendations().isEmpty());
        assertTrue(view.getRecommendations().get(0).contains("一元二次方程"));
        verify(feedbackMapper).insert(any(AiFeedback.class));
    }

    @Test
    void returnsExistingFeedbackWithoutRegenerating() {
        AiFeedback existing = new AiFeedback();
        existing.setTaskId("task-1");
        existing.setSummary("已生成");
        existing.setWeakPointsJson("[\"旧薄弱点\"]");
        existing.setRecommendationsJson("[\"旧练习\"]");
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(task());
        when(feedbackMapper.findByTaskId("task-1")).thenReturn(existing);

        AiFeedbackView view = service.getOrGenerate("task-1", "user-1");

        assertEquals("已生成", view.getSummary());
        assertEquals(Collections.singletonList("旧薄弱点"), view.getWeakPoints());
        verify(feedbackMapper, never()).insert(any(AiFeedback.class));
    }

    @Test
    void getOrGenerateRejectsCrossUserAccess() {
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(null);

        AiTaskApiException exception = assertThrows(AiTaskApiException.class,
                () -> service.getOrGenerate("task-1", "user-1"));

        assertEquals(404, exception.getHttpStatus());
        verify(feedbackMapper, never()).insert(any(AiFeedback.class));
    }

    @Test
    void generateForTaskReturnsNullWhenTaskMissing() {
        when(taskMapper.findById("task-x")).thenReturn(null);

        assertEquals(null, service.generateForTask("task-x"));
        verify(feedbackMapper, never()).insert(any(AiFeedback.class));
    }

    private AiTask task() {
        AiTask task = new AiTask();
        task.setId("task-1");
        task.setUserId("user-1");
        task.setConversationId("conv-1");
        task.setTaskType("CHATFLOW");
        task.setRequestJson("{\"query\":\"一元二次方程的求根公式\"}");
        return task;
    }

    private AiEvent event(String payload) {
        AiEvent event = new AiEvent();
        event.setTaskId("task-1");
        event.setEventType("validation_finished");
        event.setPayloadJson(payload);
        return event;
    }

    private AiMessage message(String id) {
        AiMessage message = new AiMessage();
        message.setId(id);
        message.setTaskId("task-1");
        message.setRole("assistant");
        message.setContent("一元二次方程的求根公式是 ...");
        return message;
    }
}
