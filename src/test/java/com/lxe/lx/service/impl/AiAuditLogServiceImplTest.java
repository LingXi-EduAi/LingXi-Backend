package com.lxe.lx.service.impl;

import com.lxe.lx.mapper.AiAuditLogMapper;
import com.lxe.lx.pojo.AiAuditLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAuditLogServiceImplTest {

    private final AiAuditLogMapper mapper = mock(AiAuditLogMapper.class);
    private final AiAuditLogServiceImpl service = new AiAuditLogServiceImpl(mapper);

    @Test
    void recordsAuditEntryWithAllFields() {
        AiAuditLog saved = service.record("user-1", "/api/ai/tasks", "GET", "127.0.0.1");

        assertNotNull(saved.getId());
        assertEquals("user-1", saved.getUserId());
        assertEquals("/api/ai/tasks", saved.getPath());
        assertEquals("GET", saved.getMethod());
        assertEquals("127.0.0.1", saved.getIp());
        assertNotNull(saved.getCreatedAt());
        verify(mapper).insert(saved);
    }

    @Test
    void skipsRecordWhenUserOrPathBlank() {
        assertNull(service.record(null, "/api/ai/tasks", "GET", "ip"));
        assertNull(service.record("user-1", " ", "GET", "ip"));
        verify(mapper, never()).insert(any(AiAuditLog.class));
    }

    @Test
    void deletesOlderThanCutoff() {
        when(mapper.deleteOlderThan(any(LocalDateTime.class))).thenReturn(5);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        assertEquals(5, service.deleteOlderThan(cutoff));
        verify(mapper).deleteOlderThan(cutoff);
    }

    @Test
    void returnsZeroForNullCutoff() {
        assertEquals(0, service.deleteOlderThan(null));
        verify(mapper, never()).deleteOlderThan(any(LocalDateTime.class));
    }
}
