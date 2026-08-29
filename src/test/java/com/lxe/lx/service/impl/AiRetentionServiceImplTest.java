package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiAuditLogMapper;
import com.lxe.lx.mapper.AiConversationMapper;
import com.lxe.lx.mapper.AiEventMapper;
import com.lxe.lx.mapper.AiEvidenceMapper;
import com.lxe.lx.mapper.AiMessageMapper;
import com.lxe.lx.mapper.AiModelCallLogMapper;
import com.lxe.lx.mapper.AiTaskMapper;
import com.lxe.lx.pojo.AiConversation;
import com.lxe.lx.pojo.AiTask;
import com.lxe.lx.service.AiRetentionService.PurgeResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRetentionServiceImplTest {

    private final AiMessageMapper messageMapper = mock(AiMessageMapper.class);
    private final AiEvidenceMapper evidenceMapper = mock(AiEvidenceMapper.class);
    private final AiEventMapper eventMapper = mock(AiEventMapper.class);
    private final AiAuditLogMapper auditLogMapper = mock(AiAuditLogMapper.class);
    private final AiModelCallLogMapper modelCallLogMapper = mock(AiModelCallLogMapper.class);
    private final AiTaskMapper taskMapper = mock(AiTaskMapper.class);
    private final AiConversationMapper conversationMapper = mock(AiConversationMapper.class);

    private final AiRetentionServiceImpl service = new AiRetentionServiceImpl(
            messageMapper, evidenceMapper, eventMapper, auditLogMapper, modelCallLogMapper, taskMapper,
            conversationMapper);

    @Test
    void purgesAllTablesOlderThanCutoff() {
        when(messageMapper.deleteOlderThan(any())).thenReturn(10);
        when(evidenceMapper.deleteOlderThan(any())).thenReturn(5);
        when(eventMapper.deleteOlderThan(any())).thenReturn(20);
        when(auditLogMapper.deleteOlderThan(any())).thenReturn(3);
        when(modelCallLogMapper.deleteOlderThan(any())).thenReturn(7);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        PurgeResult result = service.purgeOlderThan(cutoff);

        assertEquals(10, result.getMessages());
        assertEquals(5, result.getEvidences());
        assertEquals(20, result.getEvents());
        assertEquals(3, result.getAuditLogs());
        assertEquals(7, result.getModelCallLogs());
        assertEquals(45, result.getTotal());
        verify(messageMapper).deleteOlderThan(cutoff);
        verify(evidenceMapper).deleteOlderThan(cutoff);
        verify(eventMapper).deleteOlderThan(cutoff);
        verify(auditLogMapper).deleteOlderThan(cutoff);
        verify(modelCallLogMapper).deleteOlderThan(cutoff);
    }

    @Test
    void purgeWithNullCutoffDeletesNothing() {
        PurgeResult result = service.purgeOlderThan(null);
        assertEquals(0, result.getTotal());
        verify(messageMapper, never()).deleteOlderThan(any());
    }

    @Test
    void deletesByConversation() {
        when(conversationMapper.findByIdAndUser("conv-1", "user-1"))
                .thenReturn(new AiConversation());
        when(evidenceMapper.deleteByConversation("conv-1")).thenReturn(4);
        when(messageMapper.deleteByConversation("conv-1")).thenReturn(8);
        when(taskMapper.deleteByConversation("conv-1")).thenReturn(2);

        PurgeResult result = service.deleteByConversation("user-1", "conv-1");

        assertEquals(8, result.getMessages());
        assertEquals(4, result.getEvidences());
        assertEquals(2, result.getTasks());
        assertEquals(14, result.getTotal());
        verify(conversationMapper).findByIdAndUser("conv-1", "user-1");
        verify(evidenceMapper).deleteByConversation("conv-1");
        verify(messageMapper).deleteByConversation("conv-1");
        verify(taskMapper).deleteByConversation("conv-1");
    }

    @Test
    void deletesByTask() {
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(new AiTask());
        when(eventMapper.deleteByTask("task-1")).thenReturn(6);
        when(taskMapper.deleteByIdAndUser("task-1", "user-1")).thenReturn(1);

        PurgeResult result = service.deleteByTask("user-1", "task-1");

        assertEquals(6, result.getEvents());
        assertEquals(1, result.getTasks());
        assertEquals(7, result.getTotal());
        verify(taskMapper).findByIdAndUser("task-1", "user-1");
        verify(eventMapper).deleteByTask("task-1");
        verify(taskMapper).deleteByIdAndUser("task-1", "user-1");
    }

    @Test
    void blankConversationOrTaskDeletesNothing() {
        assertEquals(0, service.deleteByConversation("user-1", "").getTotal());
        assertEquals(0, service.deleteByTask("user-1", null).getTotal());
        verify(messageMapper, never()).deleteByConversation(any());
        verify(eventMapper, never()).deleteByTask(any());
    }

    @Test
    void deleteByConversationDeniedWhenNotOwnerDeletesNothing() {
        when(conversationMapper.findByIdAndUser("conv-1", "user-1")).thenReturn(null);

        PurgeResult result = service.deleteByConversation("user-1", "conv-1");

        assertEquals(0, result.getTotal());
        verify(conversationMapper).findByIdAndUser("conv-1", "user-1");
        verify(messageMapper, never()).deleteByConversation(any());
        verify(evidenceMapper, never()).deleteByConversation(any());
        verify(taskMapper, never()).deleteByConversation(any());
    }

    @Test
    void deleteByTaskDeniedWhenNotOwnerDeletesNothing() {
        when(taskMapper.findByIdAndUser("task-1", "user-1")).thenReturn(null);

        PurgeResult result = service.deleteByTask("user-1", "task-1");

        assertEquals(0, result.getTotal());
        verify(taskMapper).findByIdAndUser("task-1", "user-1");
        verify(eventMapper, never()).deleteByTask(any());
        verify(taskMapper, never()).deleteByIdAndUser(any(), any());
    }
}
