package com.lxe.lx.service;

import com.lxe.lx.domain.dto.LingXiEvent;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class OrderedEventBuffer {
    private final Map<Long, LingXiEvent> pending = new TreeMap<>();
    private final Consumer<LingXiEvent> sink;
    private long lastSent;
    private boolean replaying = true;

    public OrderedEventBuffer(long lastSent, Consumer<LingXiEvent> sink) {
        this.lastSent = lastSent;
        this.sink = sink;
    }

    public synchronized void addLive(LingXiEvent event) {
        add(event);
        if (!replaying) {
            drain();
        }
    }

    public synchronized void addReplay(LingXiEvent event) {
        add(event);
    }

    public synchronized void finishReplay() {
        replaying = false;
        drain();
    }

    private void add(LingXiEvent event) {
        if (event.getSequence() > lastSent) {
            pending.putIfAbsent(event.getSequence(), event);
        }
    }

    private void drain() {
        while (true) {
            LingXiEvent next = pending.remove(lastSent + 1);
            if (next == null) {
                return;
            }
            lastSent = next.getSequence();
            sink.accept(next);
        }
    }
}
