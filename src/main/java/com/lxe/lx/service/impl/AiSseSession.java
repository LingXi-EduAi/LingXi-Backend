package com.lxe.lx.service.impl;

import com.lxe.lx.domain.dto.LingXiEvent;
import com.lxe.lx.gateway.DifyStream;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

final class AiSseSession {

    private final SseEmitter emitter;
    private final TaskScheduler taskScheduler;
    private final long heartbeatMs;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object sendMonitor = new Object();
    private volatile DifyStream stream;
    private volatile ScheduledFuture<?> heartbeatFuture;

    AiSseSession(long timeoutMs, long heartbeatMs, TaskScheduler taskScheduler) {
        this(new SseEmitter(timeoutMs), heartbeatMs, taskScheduler);
    }

    AiSseSession(SseEmitter emitter, long heartbeatMs, TaskScheduler taskScheduler) {
        this.emitter = emitter;
        this.heartbeatMs = heartbeatMs;
        this.taskScheduler = taskScheduler;
        emitter.onCompletion(this::close);
        emitter.onTimeout(this::close);
        emitter.onError(error -> close());
    }

    SseEmitter getEmitter() {
        return emitter;
    }

    void attach(DifyStream stream) {
        this.stream = stream;
        if (closed.get()) {
            stream.cancel();
        }
    }

    void startHeartbeat() {
        Date firstHeartbeat = new Date(System.currentTimeMillis() + heartbeatMs);
        heartbeatFuture = taskScheduler.scheduleAtFixedRate(this::heartbeat, firstHeartbeat, heartbeatMs);
    }

    void send(LingXiEvent event) {
        if (closed.get()) {
            throw new SseConnectionClosedException();
        }
        synchronized (sendMonitor) {
            if (closed.get()) {
                throw new SseConnectionClosedException();
            }
            try {
                emitter.send(SseEmitter.event()
                        .id(event.getEventId())
                        .name(event.getEventType())
                        .data(event));
            } catch (IOException | IllegalStateException e) {
                close();
                throw new SseConnectionClosedException();
            }
        }
    }

    void complete() {
        if (closed.compareAndSet(false, true)) {
            cancelResources();
            emitter.complete();
        }
    }

    void close() {
        if (closed.compareAndSet(false, true)) {
            cancelResources();
        }
    }

    private void heartbeat() {
        if (closed.get()) {
            return;
        }
        synchronized (sendMonitor) {
            if (closed.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                close();
            }
        }
    }

    private void cancelResources() {
        ScheduledFuture<?> heartbeat = heartbeatFuture;
        if (heartbeat != null) {
            heartbeat.cancel(false);
        }
        DifyStream activeStream = stream;
        if (activeStream != null) {
            activeStream.cancel();
        }
    }

    static final class SseConnectionClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
