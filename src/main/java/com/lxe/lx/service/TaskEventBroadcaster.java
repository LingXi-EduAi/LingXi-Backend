package com.lxe.lx.service;

import com.lxe.lx.domain.dto.LingXiEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class TaskEventBroadcaster {
    private final Map<String, Map<String, Consumer<LingXiEvent>>> subscribers = new ConcurrentHashMap<>();

    public Subscription subscribe(String taskId, Consumer<LingXiEvent> consumer) {
        String subscriptionId = UUID.randomUUID().toString();
        subscribers.computeIfAbsent(taskId, ignored -> new ConcurrentHashMap<>())
                .put(subscriptionId, consumer);
        return () -> remove(taskId, subscriptionId);
    }

    public void publish(LingXiEvent event) {
        Map<String, Consumer<LingXiEvent>> taskSubscribers = subscribers.get(event.getTaskId());
        if (taskSubscribers != null) {
            taskSubscribers.values().forEach(consumer -> {
                try {
                    consumer.accept(event);
                } catch (RuntimeException ignored) {
                    // A disconnected subscriber must not affect persistence or other subscribers.
                }
            });
        }
    }

    private void remove(String taskId, String subscriptionId) {
        Map<String, Consumer<LingXiEvent>> taskSubscribers = subscribers.get(taskId);
        if (taskSubscribers != null) {
            taskSubscribers.remove(subscriptionId);
            if (taskSubscribers.isEmpty()) {
                subscribers.remove(taskId, taskSubscribers);
            }
        }
    }

    public interface Subscription {
        void close();
    }
}
