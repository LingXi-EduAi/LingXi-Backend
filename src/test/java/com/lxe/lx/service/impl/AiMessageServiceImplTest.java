package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.pojo.AiMessage;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiMessageServiceImplTest {
    private final AiMessageMapper mapper = mock(AiMessageMapper.class);
    private final AiMessageServiceImpl service = new AiMessageServiceImpl(mapper);

    @Test
    void savesUserQuestionOnlyOncePerTask() {
        AiMessage existing = new AiMessage();
        when(mapper.findByTaskAndRole("task-1", "user")).thenReturn(existing);

        assertSame(existing, service.saveUserQuestion("conversation-1", "task-1", "question"));
        verify(mapper, never()).insert(any(AiMessage.class));
    }

    @Test
    void savesAssistantAnswerWithDifyId() {
        when(mapper.findByTaskAndRole("task-1", "assistant")).thenReturn(null);
        when(mapper.findByDifyMessageId("message-1")).thenReturn(null);

        AiMessage saved = service.saveAssistantAnswer(
                "conversation-1", "task-1", "answer", "message-1");

        assertEquals("assistant", saved.getRole());
        assertEquals("message-1", saved.getDifyMessageId());
        verify(mapper).insert(saved);
    }

    @Test
    void rejectsBlankContentAndInvalidPage() {
        assertEquals(null, service.saveUserQuestion("conversation-1", "task-1", " "));
        assertTrue(service.getMessagesByConversation("conversation-1", 0, 20).isEmpty());
        verify(mapper, never()).findByConversation("conversation-1", 0, 20);

        when(mapper.findByConversation("conversation-1", 0, 20))
                .thenReturn(Collections.emptyList());
        assertTrue(service.getMessagesByConversation("conversation-1", 1, 20).isEmpty());
        verify(mapper).findByConversation("conversation-1", 0, 20);
    }
}
