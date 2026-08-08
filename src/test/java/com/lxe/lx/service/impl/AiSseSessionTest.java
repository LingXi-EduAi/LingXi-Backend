package com.lxe.lx.service.impl;

import com.lxe.lx.gateway.DifyStream;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AiSseSessionTest {

    @Test
    void closeCancelsHeartbeatAndUpstreamExactlyOnce() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> heartbeat = mock(ScheduledFuture.class);
        DifyStream stream = mock(DifyStream.class);
        doReturn(heartbeat).when(scheduler)
                .scheduleAtFixedRate(any(Runnable.class), any(Date.class), anyLong());
        AiSseSession session = new AiSseSession(new SseEmitter(), 15000, scheduler);
        session.attach(stream);
        session.startHeartbeat();

        session.close();
        session.close();

        verify(heartbeat, times(1)).cancel(false);
        verify(stream, times(1)).cancel();
        assertThrows(
                AiSseSession.SseConnectionClosedException.class,
                () -> session.send(null)
        );
    }

    @Test
    void attachingAfterCloseCancelsUpstreamImmediately() {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        DifyStream stream = mock(DifyStream.class);
        AiSseSession session = new AiSseSession(new SseEmitter(), 15000, scheduler);

        session.close();
        session.attach(stream);

        verify(stream, times(1)).cancel();
    }
}
