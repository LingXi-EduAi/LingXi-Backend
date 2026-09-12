package com.lxe.lx.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogbackPrivacyConverterTest {

    private final LogbackPrivacyConverter converter = new LogbackPrivacyConverter();

    @Test
    void masksPiiInLogMessage() {
        ILoggingEvent event = event("用户 13800138000 test@example.com 登录");

        assertEquals("用户 138****8000 te***@example.com 登录", converter.convert(event));
    }

    @Test
    void keepsPlainMessageUnchanged() {
        ILoggingEvent event = event("AI 任务 task-1 完成");

        assertEquals("AI 任务 task-1 完成", converter.convert(event));
    }

    @Test
    void returnsNullForNullMessage() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(null);

        assertNull(converter.convert(event));
    }

    private ILoggingEvent event(String message) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(message);
        return event;
    }
}
