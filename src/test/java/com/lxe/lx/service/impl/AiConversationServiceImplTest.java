package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiConversationMapper;
import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiConversation;
import com.lxe.lx.service.AiEvidenceService;
import com.lxe.lx.service.AiMessageService;
import com.lxe.lx.service.AiTaskApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiConversationServiceImplTest {
    private final AiConversationMapper mapper = mock(AiConversationMapper.class);
    private final AiConversationServiceImpl service = new AiConversationServiceImpl(
            mapper, mock(AiMessageMapper.class), mock(AiMessageService.class),
            mock(AiEvidenceService.class), mock(AiTaskMapper.class));

    @Test
    void createsConversationWithDefaultedTitle() {
        when(mapper.findByIdAndUser("conversation-1", "user-1")).thenReturn(null);

        AiConversation conversation = service.createIfAbsent(
                "conversation-1", "user-1", "  ");

        assertEquals("AI 会话", conversation.getTitle());
        assertEquals("ACTIVE", conversation.getState());
        verify(mapper).insert(conversation);
    }

    @Test
    void rejectsDeletedConversationForContinuation() {
        AiConversation conversation = new AiConversation();
        conversation.setState("DELETED");
        when(mapper.findByIdAndUser("conversation-1", "user-1")).thenReturn(conversation);

        assertThrows(AiTaskApiException.class,
                () -> service.requireActiveOwned("conversation-1", "user-1"));
    }

    @Test
    void deleteIsIdempotentForAlreadyDeletedConversation() {
        AiConversation conversation = new AiConversation();
        conversation.setState("DELETED");
        when(mapper.findByIdAndUser("conversation-1", "user-1")).thenReturn(conversation);

        service.delete("conversation-1", "user-1");

        verify(mapper, never()).softDelete(any(AiConversation.class));
    }
}
